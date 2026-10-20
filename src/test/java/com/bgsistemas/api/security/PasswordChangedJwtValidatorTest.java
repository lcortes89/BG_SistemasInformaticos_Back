package com.bgsistemas.api.security;

import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// BGSI-272: tokens issued before the last password change are not valid
@ExtendWith(MockitoExtension.class)
class PasswordChangedJwtValidatorTest {

    private static final String EMAIL = "admin@mail.com";
    private static final Instant CHANGED_AT = Instant.parse("2026-10-20T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PasswordChangedJwtValidator validator;

    private Jwt tokenIssuedAt(Instant issuedAt) {
        return Jwt.withTokenValue("token").header("alg", "HS512")
                .subject(EMAIL).issuedAt(issuedAt).expiresAt(issuedAt.plusSeconds(3600)).build();
    }

    private User adminWithPasswordChangedAt(Instant changedAt) {
        User user = new User(EMAIL, "hash", Role.ADMIN);
        user.setPasswordChangedAt(changedAt);
        return user;
    }

    @Test
    void acceptsTheTokenWhenThePasswordWasNeverChanged() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(adminWithPasswordChangedAt(null)));

        assertThat(validator.validate(tokenIssuedAt(CHANGED_AT)).hasErrors()).isFalse();
    }

    @Test
    void acceptsATokenIssuedAfterOrAtTheSameSecondAsTheChange() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(adminWithPasswordChangedAt(CHANGED_AT)));

        assertThat(validator.validate(tokenIssuedAt(CHANGED_AT)).hasErrors()).isFalse();
        assertThat(validator.validate(tokenIssuedAt(CHANGED_AT.plusSeconds(5))).hasErrors()).isFalse();
    }

    @Test
    void rejectsATokenIssuedBeforeTheChange() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(adminWithPasswordChangedAt(CHANGED_AT)));

        assertThat(validator.validate(tokenIssuedAt(CHANGED_AT.minusSeconds(1))).hasErrors()).isTrue();
    }

    @Test
    void rejectsATokenOfAUserThatNoLongerExists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(validator.validate(tokenIssuedAt(CHANGED_AT)).hasErrors()).isTrue();
    }
}
