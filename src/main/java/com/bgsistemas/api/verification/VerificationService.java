package com.bgsistemas.api.verification;

import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.passwordreset.PasswordResetServiceImpl;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.bgsistemas.api.user.UserStatus;
import com.bgsistemas.api.verification.exceptions.InvalidVerificationTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

// BGSI-238: a new admin must prove the email is theirs before the account can be used
@Service
public class VerificationService {

    static final Duration LINK_VALIDITY = Duration.ofHours(24);
    static final long MAX_VALID_LINKS = 3;

    private final VerificationTokenRepository tokenRepository;
    private final PasswordResetEmailSender emailSender;
    private final UserRepository userRepository;

    public VerificationService(VerificationTokenRepository tokenRepository, PasswordResetEmailSender emailSender,
                               UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
        this.userRepository = userRepository;
    }

    @Transactional
    public void sendVerification(User user) {
        String token = PasswordResetServiceImpl.newToken();
        Instant expiresAt = Instant.now().plus(LINK_VALIDITY);
        tokenRepository.save(new VerificationToken(user, PasswordResetServiceImpl.hash(token), expiresAt));
        emailSender.sendEmailVerification(user.getEmail(), token); // @Async: the answer does not wait for Gmail
    }

    @Transactional
    public void verifyEmail(String token) {
        Instant now = Instant.now();
        VerificationToken verificationToken = tokenRepository.findByTokenHash(PasswordResetServiceImpl.hash(token))
                .filter(found -> found.isUsable(now))
                .orElseThrow(InvalidVerificationTokenException::new);

        User user = verificationToken.getUser();
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new InvalidVerificationTokenException(); // an old link cannot unblock an admin
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        verificationToken.markUsed(now); // single use
    }

    // BGSI-238: the page of an expired link sends its old token to get a new one.
    // Always the same answer (no error), so nobody can find out which accounts exist
    @Transactional
    public void resendVerification(String oldToken) {
        tokenRepository.findByTokenHash(PasswordResetServiceImpl.hash(oldToken))
                .map(VerificationToken::getUser)
                .filter(user -> user.getStatus() == UserStatus.PENDING) // active or blocked: nothing to send
                .filter(user -> tokenRepository.countByUserAndExpiresAtAfter(user, Instant.now()) < MAX_VALID_LINKS)
                .ifPresent(this::sendVerification);
    }
}
