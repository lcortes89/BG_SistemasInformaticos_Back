package com.bgsistemas.api.passwordreset;

import com.bgsistemas.api.passwordreset.exceptions.InvalidResetTokenException;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    static final Duration LINK_VALIDITY = Duration.ofMinutes(30);
    static final Duration REQUEST_WINDOW = Duration.ofMinutes(15);
    static final int MAX_REQUESTS_PER_WINDOW = 3;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailSender emailSender;

    public PasswordResetServiceImpl(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                    PasswordEncoder passwordEncoder, PasswordResetEmailSender emailSender) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
    }

    @Override
    @Transactional
    public void requestReset(String email) {
        Optional<User> found = userRepository.findByEmail(email.trim());
        if (found.isEmpty()) {
            return; // same answer as for a real admin: nobody can find out which emails exist
        }
        User user = found.get();
        Instant now = Instant.now();

        if (tokenRepository.countByUserAndCreatedAtAfter(user, now.minus(REQUEST_WINDOW)) >= MAX_REQUESTS_PER_WINDOW) {
            return; // too many emails in a short time: we stop sending (no error, same answer)
        }

        String token = newToken();
        tokenRepository.save(new PasswordResetToken(user, hash(token), now, now.plus(LINK_VALIDITY)));
        emailSender.send(user.getEmail(), token); // @Async: the answer does not wait for Gmail
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hash(token))
                .filter(found -> found.isUsable(now))
                .orElseThrow(InvalidResetTokenException::new);

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        // Like in "Mi cuenta" (BGSI-272): every session opened before now stops working
        user.setPasswordChangedAt(now.truncatedTo(ChronoUnit.SECONDS));
        userRepository.save(user);

        // This link and any other link still pending for this user can no longer be used
        tokenRepository.findByUserAndUsedAtIsNull(user).forEach(pending -> pending.markUsed(now));
        resetToken.markUsed(now);
    }

    // 32 random bytes -> 43 characters that are safe in a URL
    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
