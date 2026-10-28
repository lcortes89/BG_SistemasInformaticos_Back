package com.bgsistemas.api.security;

import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

// BGSI-272: a JWT issued before the last password change is rejected (401),
// so changing the password closes the sessions open on other devices
@Component
public class PasswordChangedJwtValidator implements OAuth2TokenValidator<Jwt> {

    private final UserRepository userRepository;

    public PasswordChangedJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Optional<User> user = userRepository.findByEmail(jwt.getSubject());
        if (user.isEmpty()) {
            return invalid("The user of this token no longer exists");
        }
        // BGSI-277: blocking an admin also closes the sessions that are already open
        if (!user.get().isActive()) {
            return invalid("This account is blocked");
        }

        Instant changedAt = user.get().getPasswordChangedAt();
        Instant issuedAt = jwt.getIssuedAt();
        if (changedAt != null && (issuedAt == null || issuedAt.isBefore(changedAt))) {
            return invalid("The password has changed, please log in again");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult invalid(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
