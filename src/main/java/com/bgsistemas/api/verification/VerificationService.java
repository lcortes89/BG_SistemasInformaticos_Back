package com.bgsistemas.api.verification;

import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.passwordreset.PasswordResetServiceImpl;
import com.bgsistemas.api.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

// BGSI-238: a new admin must prove the email is theirs before the account can be used
@Service
public class VerificationService {

    static final Duration LINK_VALIDITY = Duration.ofHours(24);

    private final VerificationTokenRepository tokenRepository;
    private final PasswordResetEmailSender emailSender;

    public VerificationService(VerificationTokenRepository tokenRepository, PasswordResetEmailSender emailSender) {
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
    }

    @Transactional
    public void sendVerification(User user) {
        String token = PasswordResetServiceImpl.newToken();
        Instant expiresAt = Instant.now().plus(LINK_VALIDITY);
        tokenRepository.save(new VerificationToken(user, PasswordResetServiceImpl.hash(token), expiresAt));
        emailSender.sendEmailVerification(user.getEmail(), token); // @Async: the answer does not wait for Gmail
    }
}