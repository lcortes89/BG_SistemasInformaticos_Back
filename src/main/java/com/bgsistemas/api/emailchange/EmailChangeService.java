package com.bgsistemas.api.emailchange;

import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import com.bgsistemas.api.emailchange.dtos.PendingEmailChangeDTO;
import com.bgsistemas.api.emailchange.exceptions.InvalidEmailChangeTokenException;
import com.bgsistemas.api.globals.ResourceNotFoundException;
import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.passwordreset.PasswordResetServiceImpl;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

// BGSI-299: each admin changes their own email in "Mi cuenta" (nobody changes the email of another admin).
// 1) request: a link goes to the NEW address and a notice to the OLD one. The email does not change yet.
// 2) confirm: the link changes the email, closes every session and tells all the active admins
@Service
public class EmailChangeService {

    static final Duration LINK_VALIDITY = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final EmailChangeTokenRepository tokenRepository;
    private final PasswordResetEmailSender emailSender;

    public EmailChangeService(UserRepository userRepository, EmailChangeTokenRepository tokenRepository,
                              PasswordResetEmailSender emailSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
    }

    @Transactional
    public PendingEmailChangeDTO request(String currentEmail, String newEmail) {
        User user = find(currentEmail);
        String clean = newEmail.trim().toLowerCase(Locale.ROOT);
        if (clean.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("The new email is the same as the current one");
        }
        if (userRepository.existsByEmail(clean)) {
            throw new EmailAlreadyExistsException(clean);
        }
        Instant now = Instant.now();
        cancelPending(user, now); // only the last link works
        String token = PasswordResetServiceImpl.newToken();
        EmailChangeToken saved = tokenRepository.save(new EmailChangeToken(user, clean,
                PasswordResetServiceImpl.hash(token), now, now.plus(LINK_VALIDITY)));
        emailSender.sendEmailChangeLink(clean, token, user.getEmail()); // @Async
        emailSender.sendEmailChangeRequested(user.getEmail(), clean); // @Async: if it was not you, you know it
        return new PendingEmailChangeDTO(saved.getNewEmail(), saved.getExpiresAt());
    }

    // The change waiting for the link (to show it in "Mi cuenta")
    @Transactional(readOnly = true)
    public PendingEmailChangeDTO pending(String currentEmail) {
        Instant now = Instant.now();
        return tokenRepository.findByUserAndUsedAtIsNull(find(currentEmail)).stream()
                .filter(token -> token.isUsable(now))
                .max(Comparator.comparing(EmailChangeToken::getCreatedAt))
                .map(token -> new PendingEmailChangeDTO(token.getNewEmail(), token.getExpiresAt()))
                .orElse(PendingEmailChangeDTO.none());
    }

    @Transactional
    public void cancel(String currentEmail) {
        cancelPending(find(currentEmail), Instant.now());
    }

    // Opened from the email: it works even without a session. Returns the new email
    @Transactional
    public String confirm(String token) {
        Instant now = Instant.now();
        EmailChangeToken link = tokenRepository.findByTokenHash(PasswordResetServiceImpl.hash(token))
                .filter(found -> found.isUsable(now))
                .orElseThrow(InvalidEmailChangeTokenException::new);
        User user = link.getUser();
        if (!user.isActive()) {
            throw new InvalidEmailChangeTokenException(); // a blocked admin cannot change anything
        }
        String newEmail = link.getNewEmail();
        if (userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException(newEmail); // another admin took it meanwhile
        }
        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        // Like a password change (BGSI-272): every open session stops working
        user.setPasswordChangedAt(now.truncatedTo(ChronoUnit.SECONDS));
        userRepository.save(user);
        cancelPending(user, now);
        link.markUsed(now);
        notifyAdmins(oldEmail + " ha cambiado su email de administrador. Ahora es " + newEmail + ".", oldEmail);
        return newEmail;
    }

    private User find(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User " + email + " not found"));
    }

    private void cancelPending(User user, Instant now) {
        tokenRepository.findByUserAndUsedAtIsNull(user).forEach(pending -> pending.markUsed(now));
    }

    // All the active admins (the new email is one of them now) and the old address
    private void notifyAdmins(String message, String oldEmail) {
        List<String> to = Stream.concat(
                        userRepository.findByRole(Role.ADMIN).stream().filter(User::isActive).map(User::getEmail),
                        Stream.of(oldEmail))
                .distinct()
                .toList();
        emailSender.sendAdminNotice(to, message); // @Async
    }
}
