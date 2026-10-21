package com.bgsistemas.api.account;

import com.bgsistemas.api.account.exceptions.InvalidPasswordChangeException;
import com.bgsistemas.api.security.ConfirmationTokenService;
import com.bgsistemas.api.security.LoginAttemptService;
import com.bgsistemas.api.security.TooManyAttemptsException;
import com.bgsistemas.api.twofactor.InvalidCodeException;
import com.bgsistemas.api.twofactor.TwoFactorService;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// BGSI-275: typing the password again gives a 5-minute token; wrong passwords count for the lock
class PasswordConfirmationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final LoginAttemptService loginAttempts = new LoginAttemptService();
    private final ConfirmationTokenService confirmationTokens = new ConfirmationTokenService();
    private final TwoFactorService twoFactorService = mock(TwoFactorService.class);
    private final PasswordConfirmationService service = new PasswordConfirmationService(
            userRepository, passwordEncoder, loginAttempts, confirmationTokens, twoFactorService);
    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User("admin@mail.com", passwordEncoder.encode("Correct2026"), Role.ADMIN);
        when(userRepository.findByEmail("admin@mail.com")).thenReturn(Optional.of(admin));
    }

    @Test
    void theRightPasswordGivesAValidToken() {
        String token = service.confirm("admin@mail.com", "Correct2026", null);

        assertThat(confirmationTokens.isValid(token, "admin@mail.com", Instant.now())).isTrue();
    }

    @Test
    void aWrongPasswordIsRejectedOnThePasswordField() {
        assertThatThrownBy(() -> service.confirm("admin@mail.com", "Wrong2026", null))
                .isInstanceOf(InvalidPasswordChangeException.class)
                .hasMessage("Password is not correct")
                .extracting("field").isEqualTo("password");
    }

    @Test
    void fiveWrongPasswordsLockTheEmailEvenWithTheRightOne() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.confirm("admin@mail.com", "Wrong2026", null))
                    .isInstanceOf(InvalidPasswordChangeException.class);
        }

        assertThatThrownBy(() -> service.confirm("admin@mail.com", "Correct2026", null))
                .isInstanceOf(TooManyAttemptsException.class)
                .hasMessage("Too many failed attempts. Try again in 15 minutes");
    }

    @Test
    void theRightPasswordStartsTheCountAgain() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.confirm("admin@mail.com", "Wrong2026", null))
                    .isInstanceOf(InvalidPasswordChangeException.class);
        }
        service.confirm("admin@mail.com", "Correct2026", null);

        assertThat(loginAttempts.lockRemaining("admin@mail.com", Instant.now())).isEmpty();
    }

    // BGSI-276: with 2FA on, the password is not enough
    @Test
    void withTwoFactorOnTheCodeIsNeededToo() {
        admin.setTotpEnabled(true);

        assertThatThrownBy(() -> service.confirm("admin@mail.com", "Correct2026", null))
                .isInstanceOf(InvalidPasswordChangeException.class)
                .hasMessage("Code is required");
    }

    @Test
    void withTwoFactorOnAWrongCodeIsRejectedAndCounted() {
        admin.setTotpEnabled(true);
        when(twoFactorService.verify(eq(admin), eq("111111"), any())).thenReturn(false);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.confirm("admin@mail.com", "Correct2026", "111111"))
                    .isInstanceOf(InvalidCodeException.class)
                    .hasMessage("Code is not correct");
        }

        assertThat(loginAttempts.lockRemaining("admin@mail.com", Instant.now())).isPresent();
    }

    @Test
    void withTwoFactorOnPasswordAndCodeGiveTheToken() {
        admin.setTotpEnabled(true);
        when(twoFactorService.verify(eq(admin), eq("123456"), any())).thenReturn(true);

        String token = service.confirm("admin@mail.com", "Correct2026", "123456");

        assertThat(confirmationTokens.isValid(token, "admin@mail.com", Instant.now())).isTrue();
    }
}
