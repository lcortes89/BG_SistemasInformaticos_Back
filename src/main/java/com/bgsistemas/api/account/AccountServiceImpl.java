package com.bgsistemas.api.account;

import com.bgsistemas.api.account.exceptions.InvalidPasswordChangeException;
import com.bgsistemas.api.auth.JwtService;
import com.bgsistemas.api.globals.ResourceNotFoundException;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AccountServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public String changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User " + email + " not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordChangeException("currentPassword", "Current password is not correct");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new InvalidPasswordChangeException("newPassword", "New password must be different from the current one");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        // Whole seconds, like the "iat" of the JWT, so a token issued right after is still valid
        user.setPasswordChangedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        userRepository.save(user);

        return jwtService.generateToken(user);
    }
}
