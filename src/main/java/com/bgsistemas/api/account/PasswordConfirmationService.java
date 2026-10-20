package com.bgsistemas.api.account;

import com.bgsistemas.api.account.exceptions.InvalidPasswordChangeException;
import com.bgsistemas.api.globals.ResourceNotFoundException;
import com.bgsistemas.api.security.ConfirmationTokenService;
import com.bgsistemas.api.security.LoginAttemptService;
import com.bgsistemas.api.security.TooManyAttemptsException;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

// BGSI-275: the admin types the password again before a sensitive action.
// Wrong passwords count for the same lock as the login (5 failures -> 15 minutes),
// so an open session cannot be used to guess the password.
@Service
public class PasswordConfirmationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;
    private final ConfirmationTokenService confirmationTokens;

    public PasswordConfirmationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                       LoginAttemptService loginAttempts, ConfirmationTokenService confirmationTokens) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
        this.confirmationTokens = confirmationTokens;
    }

    // Returns a token valid for 5 minutes (ConfirmationTokenService.VALIDITY)
    public String confirm(String email, String password) {
        Instant now = Instant.now();
        Optional<Duration> locked = loginAttempts.lockRemaining(email, now);
        if (locked.isPresent()) {
            throw new TooManyAttemptsException(locked.get());
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User " + email + " not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginAttempts.onFailure(email, now);
            throw new InvalidPasswordChangeException("password", "Password is not correct");
        }

        loginAttempts.onSuccess(email);
        return confirmationTokens.issue(email, now);
    }
}
