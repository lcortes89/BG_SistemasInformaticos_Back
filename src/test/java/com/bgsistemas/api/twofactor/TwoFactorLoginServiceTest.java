package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.auth.JwtService;
import com.bgsistemas.api.auth.dtos.LoginResponseDTO;
import com.bgsistemas.api.security.LoginAttemptService;
import com.bgsistemas.api.security.TooManyAttemptsException;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// BGSI-276: second step of the login
class TwoFactorLoginServiceTest {

    private final PendingLoginService pendingLogins = new PendingLoginService();
    private final TwoFactorService twoFactorService = mock(TwoFactorService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LoginAttemptService loginAttempts = new LoginAttemptService();
    private final JwtService jwtService = mock(JwtService.class);
    private final TwoFactorLoginService service =
            new TwoFactorLoginService(pendingLogins, twoFactorService, userRepository, loginAttempts, jwtService);

    private final User admin = new User("luisa@bg.es", "hashed", Role.ADMIN);
    private String pendingToken;

    @BeforeEach
    void setUp() {
        admin.setTotpEnabled(true);
        when(userRepository.findByEmail("luisa@bg.es")).thenReturn(Optional.of(admin));
        when(jwtService.generateToken(admin)).thenReturn("jwt");
        pendingToken = pendingLogins.start("luisa@bg.es", Instant.now());
    }

    @Test
    void theRightCodeGivesTheJwtAndTheTokenCannotBeUsedAgain() {
        when(twoFactorService.verify(eq(admin), eq("123456"), any())).thenReturn(true);

        LoginResponseDTO response = service.verify(pendingToken, "123456");

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.email()).isEqualTo("luisa@bg.es");
        assertThat(pendingLogins.emailFor(pendingToken, Instant.now())).isEmpty();
    }

    @Test
    void aWrongCodeIsRejectedAndTheAdminCanTryAgain() {
        when(twoFactorService.verify(eq(admin), eq("000000"), any())).thenReturn(false);

        assertThatThrownBy(() -> service.verify(pendingToken, "000000"))
                .isInstanceOf(InvalidCodeException.class)
                .hasMessage("Code is not correct");
        assertThat(pendingLogins.emailFor(pendingToken, Instant.now())).contains("luisa@bg.es");
    }

    @Test
    void fiveWrongCodesLockTheLogin() {
        when(twoFactorService.verify(eq(admin), eq("000000"), any())).thenReturn(false);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.verify(pendingToken, "000000")).isInstanceOf(InvalidCodeException.class);
        }

        assertThatThrownBy(() -> service.verify(pendingToken, "123456")).isInstanceOf(TooManyAttemptsException.class);
        assertThat(pendingLogins.emailFor(pendingToken, Instant.now())).isEmpty();
    }

    @Test
    void anExpiredOrMadeUpLoginMustStartAgain() {
        assertThatThrownBy(() -> service.verify("made-up", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The login has expired. Please log in again");
    }
}
