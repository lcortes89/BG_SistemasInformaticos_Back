package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.globals.ResourceConflictException;
import com.bgsistemas.api.globals.ResourceNotFoundException;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;

// BGSI-276: set up, turn on and off the two-factor authentication, and check the codes
@Service
public class TwoFactorService {

    static final int RECOVERY_CODES = 10;
    // No 0/o, 1/l/i: easy to copy by hand
    private static final String RECOVERY_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

    private final UserRepository userRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final TotpGenerator totp;
    private final SecretCipher cipher;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public TwoFactorService(UserRepository userRepository, RecoveryCodeRepository recoveryCodeRepository,
                            TotpGenerator totp, SecretCipher cipher, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.totp = totp;
        this.cipher = cipher;
        this.passwordEncoder = passwordEncoder;
    }

    public record Status(boolean enabled, long recoveryCodesLeft) {
    }

    public record Setup(String secret, String otpauthUri) {
    }

    public Status status(String email) {
        User user = find(email);
        long left = user.isTotpEnabled() ? recoveryCodeRepository.countByUserAndUsedAtIsNull(user) : 0;
        return new Status(user.isTotpEnabled(), left);
    }

    // Step 1: a new secret for the QR. It is saved but not active until a code confirms it (enable)
    public Setup setup(String email) {
        User user = find(email);
        if (user.isTotpEnabled()) {
            throw new ResourceConflictException("Two-factor authentication is already on");
        }
        String secret = totp.newSecret();
        user.setTotpSecret(cipher.encrypt(secret));
        user.setTotpLastStep(null);
        userRepository.save(user);
        return new Setup(secret, totp.otpauthUri(secret, user.getEmail()));
    }

    // Step 2: the first code proves the app works. Returns the 10 recovery codes (shown only this time)
    @Transactional
    public List<String> enable(String email, String code) {
        User user = find(email);
        if (user.isTotpEnabled()) {
            throw new ResourceConflictException("Two-factor authentication is already on");
        }
        if (user.getTotpSecret() == null) {
            throw new IllegalArgumentException("Start the set up first");
        }
        OptionalLong step = totp.matchingStep(cipher.decrypt(user.getTotpSecret()), code, Instant.now());
        if (step.isEmpty()) {
            throw new InvalidCodeException();
        }
        user.setTotpEnabled(true);
        user.setTotpLastStep(step.getAsLong());
        userRepository.save(user);
        return newRecoveryCodes(user);
    }

    // The action is protected with @RequiresConfirmation: password + code were already checked
    @Transactional
    public void disable(String email) {
        User user = find(email);
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        user.setTotpLastStep(null);
        userRepository.save(user);
        recoveryCodeRepository.deleteByUser(user);
    }

    // A 6-digit code of the app or one recovery code. A used code (or step) does not work again
    @Transactional
    public boolean verify(User user, String code, Instant now) {
        if (!user.isTotpEnabled() || code == null || code.isBlank()) {
            return false;
        }
        String clean = code.replaceAll("[\\s-]", "").toLowerCase(Locale.ROOT);
        if (clean.matches("\\d{" + TotpGenerator.DIGITS + "}")) {
            OptionalLong step = totp.matchingStep(cipher.decrypt(user.getTotpSecret()), clean, now);
            Long lastStep = user.getTotpLastStep();
            if (step.isEmpty() || (lastStep != null && step.getAsLong() <= lastStep)) {
                return false;
            }
            user.setTotpLastStep(step.getAsLong());
            userRepository.save(user);
            return true;
        }
        for (RecoveryCode recoveryCode : recoveryCodeRepository.findByUserAndUsedAtIsNull(user)) {
            if (passwordEncoder.matches(clean, recoveryCode.getCodeHash())) {
                recoveryCode.markUsed(now);
                recoveryCodeRepository.save(recoveryCode);
                return true;
            }
        }
        return false;
    }

    private List<String> newRecoveryCodes(User user) {
        recoveryCodeRepository.deleteByUser(user);
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODES; i++) {
            String code = randomCode();
            recoveryCodeRepository.save(new RecoveryCode(user, passwordEncoder.encode(code.replace("-", ""))));
            codes.add(code);
        }
        return codes;
    }

    // e.g. "k7mp-3xqa"
    private String randomCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                code.append('-');
            }
            code.append(RECOVERY_ALPHABET.charAt(random.nextInt(RECOVERY_ALPHABET.length())));
        }
        return code.toString();
    }

    private User find(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User " + email + " not found"));
    }
}
