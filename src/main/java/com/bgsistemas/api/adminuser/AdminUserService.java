package com.bgsistemas.api.adminuser;

import com.bgsistemas.api.adminuser.dtos.AdminUserDTO;
import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import com.bgsistemas.api.globals.ResourceConflictException;
import com.bgsistemas.api.globals.ResourceNotFoundException;
import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.passwordreset.PasswordResetService;
import com.bgsistemas.api.passwordreset.PasswordResetTokenRepository;
import com.bgsistemas.api.twofactor.RecoveryCodeRepository;
import com.bgsistemas.api.twofactor.TwoFactorService;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.bgsistemas.api.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

// BGSI-277: "Administradores". Nobody can act on their own account here (that is "Mi cuenta"),
// so there is always at least one active admin: the one doing the action.
// Every change is emailed to all the active admins
@Service
public class AdminUserService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final TwoFactorService twoFactorService;
    private final PasswordResetEmailSender emailSender;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordResetService passwordResetService,
                            PasswordResetTokenRepository resetTokenRepository, RecoveryCodeRepository recoveryCodeRepository,
                            TwoFactorService twoFactorService, PasswordResetEmailSender emailSender,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetService = passwordResetService;
        this.resetTokenRepository = resetTokenRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.twoFactorService = twoFactorService;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminUserDTO> list(String currentEmail) {
        return userRepository.findByRole(Role.ADMIN).stream()
                .sorted(Comparator.comparing(User::getEmail))
                .map(user -> toDTO(user, currentEmail))
                .toList();
    }

    // Creates the admin as PENDING with a random password nobody knows and emails the invitation
    @Transactional
    public AdminUserDTO invite(String email, String currentEmail) {
        String clean = normalize(email);
        if (userRepository.existsByEmail(clean)) {
            throw new EmailAlreadyExistsException(clean);
        }
        User user = new User(clean, passwordEncoder.encode(randomPassword()), Role.ADMIN);
        user.setStatus(UserStatus.PENDING);
        User saved = userRepository.save(user);
        passwordResetService.invite(saved, currentEmail);
        notifyAdmins(currentEmail + " ha invitado a " + clean + " como administrador.");
        return toDTO(saved, currentEmail);
    }

    @Transactional
    public void resendInvitation(Long id, String currentEmail) {
        User user = other(id, currentEmail);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new ResourceConflictException("This admin has already accepted the invitation");
        }
        passwordResetService.invite(user, currentEmail);
    }

    @Transactional
    public AdminUserDTO block(Long id, String currentEmail) {
        User user = other(id, currentEmail);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResourceConflictException("Only an active admin can be blocked");
        }
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        notifyAdmins(currentEmail + " ha bloqueado a " + user.getEmail() + ". Ya no puede entrar al panel.");
        return toDTO(user, currentEmail);
    }

    @Transactional
    public AdminUserDTO unblock(Long id, String currentEmail) {
        User user = other(id, currentEmail);
        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new ResourceConflictException("This admin is not blocked");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        notifyAdmins(currentEmail + " ha desbloqueado a " + user.getEmail() + ". Puede volver a entrar al panel.");
        return toDTO(user, currentEmail);
    }

    // For an admin who lost the phone and the recovery codes
    @Transactional
    public AdminUserDTO resetTwoFactor(Long id, String currentEmail) {
        User user = other(id, currentEmail);
        twoFactorService.disable(user.getEmail());
        notifyAdmins(currentEmail + " ha quitado la verificación en dos pasos de " + user.getEmail() + ".");
        return toDTO(userRepository.findById(id).orElseThrow(), currentEmail);
    }

    // The sessions of that admin are closed; the old and the new address are told too
    @Transactional
    public AdminUserDTO changeEmail(Long id, String newEmail, String currentEmail) {
        User user = other(id, currentEmail);
        String clean = normalize(newEmail);
        String oldEmail = user.getEmail();
        if (clean.equals(oldEmail)) {
            return toDTO(user, currentEmail);
        }
        if (userRepository.existsByEmail(clean)) {
            throw new EmailAlreadyExistsException(clean);
        }
        user.setEmail(clean);
        user.setPasswordChangedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        userRepository.save(user);
        String message = currentEmail + " ha cambiado el email de un administrador: " + oldEmail + " ahora es " + clean + ".";
        notifyAdmins(message, oldEmail, clean);
        return toDTO(user, currentEmail);
    }

    @Transactional
    public void delete(Long id, String currentEmail) {
        User user = other(id, currentEmail);
        recoveryCodeRepository.deleteByUser(user);
        resetTokenRepository.deleteByUser(user);
        userRepository.delete(user);
        notifyAdmins(currentEmail + " ha borrado la cuenta de administrador " + user.getEmail() + ".");
    }

    // Any admin except the one logged in
    private User other(Long id, String currentEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin " + id + " not found"));
        if (user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new ResourceConflictException("You cannot do this to your own account. Use \"Mi cuenta\"");
        }
        return user;
    }

    private void notifyAdmins(String message, String... alsoTo) {
        List<String> to = Stream.concat(
                        userRepository.findByRole(Role.ADMIN).stream().filter(User::isActive).map(User::getEmail),
                        Stream.of(alsoTo))
                .distinct()
                .toList();
        emailSender.sendAdminNotice(to, message); // @Async: the answer does not wait for Gmail
    }

    private AdminUserDTO toDTO(User user, String currentEmail) {
        return new AdminUserDTO(user.getId(), user.getEmail(), user.getStatus().name(), user.isTotpEnabled(),
                user.getLastLoginAt(), user.getEmail().equalsIgnoreCase(currentEmail));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String randomPassword() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
