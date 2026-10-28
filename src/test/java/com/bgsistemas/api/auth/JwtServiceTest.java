package com.bgsistemas.api.auth;

import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-only-for-tests-it-must-be-at-least-64-characters-long-0123456789";

    private final JwtService jwtService =
            new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(SECRET.getBytes())), 2);

    private final JwtDecoder decoder = NimbusJwtDecoder
            .withSecretKey(new SecretKeySpec(SECRET.getBytes(), "HmacSHA512"))
            .macAlgorithm(MacAlgorithm.HS512)
            .build();

    @Test
    void shouldGenerateTokenWithEmailRoleAndExpiration() {
        User admin = new User("admin@mail.com", "hashed", Role.ADMIN);

        String token = jwtService.generateToken(admin);
        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("admin@mail.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }
}
