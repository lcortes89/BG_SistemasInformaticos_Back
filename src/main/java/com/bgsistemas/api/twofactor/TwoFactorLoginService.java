package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.auth.JwtService;
import com.bgsistemas.api.auth.dtos.LoginResponseDTO;
import com.bgsistemas.api.globals.ResourceNotFoundException;
import com.bgsistemas.api.security.LoginAttemptService;
import com.bgsistemas.api.security.TooManyAttemptsException;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

// BGSI-276: second step of the login. Wrong codes count for the same lock as the passwords (BGSI-274)
@Service
public class TwoFactorLoginService {

    private final PendingLoginService pendingLogins;
    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;
    private final LoginAttemptService loginAttempts;
    private final JwtService jwtService;

    public TwoFactorLoginService(PendingLoginService pendingLogins, TwoFactorService twoFactorService,
                                 UserRepository userRepository, LoginAttemptService loginAttempts, JwtService jwtService) {
        this.pendingLogins = pendingLogins;
        this.twoFactorService = twoFactorService;
        this.userRepository = userRepository;
        this.loginAttempts = loginAttempts;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO verify(String pendingToken, String code) {
        Instant now = Instant.now();
        String email = pendingLogins.emailFor(pendingToken, now)
                .orElseThrow(() -> new IllegalArgumentException("The login has expired. Please log in again"));

        Optional<Duration> locked = loginAttempts.lockRemaining(email, now);
        if (locked.isPresent()) {
            pendingLogins.finish(pendingToken);
            throw new TooManyAttemptsException(locked.get());
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User " + email + " not found"));
        if (!twoFactorService.verify(user, code, now)) {
            loginAttempts.onFailure(email, now);
            throw new InvalidCodeException();
        }

        loginAttempts.onSuccess(email);
        pendingLogins.finish(pendingToken);
        user.setLastLoginAt(now); // BGSI-277
        userRepository.save(user);
        return new LoginResponseDTO(jwtService.generateToken(user), user.getEmail(), user.getRole().name());
    }
}
