package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.globals.ResourceConflictException;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// BGSI-276: set up with a QR, turn on with the first code, 10 recovery codes, codes used only once
class TwoFactorServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RecoveryCodeRepository recoveryCodeRepository = mock(RecoveryCodeRepository.class);
    private final TotpGenerator totp = new TotpGenerator();
    private final SecretCipher cipher = new SecretCipher("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final TwoFactorService service =
            new TwoFactorService(userRepository, recoveryCodeRepository, totp, cipher, passwordEncoder);

    private final User admin = new User("luisa@bg.es", "hashed", Role.ADMIN);
    private final List<RecoveryCode> savedCodes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(userRepository.findByEmail("luisa@bg.es")).thenReturn(Optional.of(admin));
        when(recoveryCodeRepository.save(any())).thenAnswer(call -> {
            RecoveryCode code = call.getArgument(0);
            if (!savedCodes.contains(code)) savedCodes.add(code);
            return code;
        });
        when(recoveryCodeRepository.findByUserAndUsedAtIsNull(admin))
                .thenAnswer(call -> savedCodes.stream().filter(code -> code.getUsedAt() == null).toList());
    }

    private String secretOf(User user) {
        return cipher.decrypt(user.getTotpSecret());
    }

    private String codeNow(int stepsLater) {
        return totp.codeAt(secretOf(admin), totp.stepAt(Instant.now()) + stepsLater);
    }

    private List<String> turnOn() {
        service.setup("luisa@bg.es");
        return service.enable("luisa@bg.es", codeNow(0));
    }

    @Test
    void setupSavesTheSecretEncryptedButDoesNotTurnItOn() {
        TwoFactorService.Setup setup = service.setup("luisa@bg.es");

        assertThat(admin.getTotpSecret()).isNotEqualTo(setup.secret());
        assertThat(secretOf(admin)).isEqualTo(setup.secret());
        assertThat(setup.otpauthUri()).contains("secret=" + setup.secret());
        assertThat(admin.isTotpEnabled()).isFalse();
    }

    @Test
    void theFirstRightCodeTurnsItOnAndGivesTenRecoveryCodes() {
        List<String> codes = turnOn();

        assertThat(admin.isTotpEnabled()).isTrue();
        assertThat(codes).hasSize(10).allMatch(code -> code.matches("[a-z2-9]{4}-[a-z2-9]{4}"));
        assertThat(savedCodes).hasSize(10).noneMatch(saved -> codes.contains(saved.getCodeHash()));
        verify(recoveryCodeRepository).deleteByUser(admin);
    }

    @Test
    void aWrongFirstCodeDoesNotTurnItOn() {
        service.setup("luisa@bg.es");

        assertThatThrownBy(() -> service.enable("luisa@bg.es", "000000")).isInstanceOf(InvalidCodeException.class);
        assertThat(admin.isTotpEnabled()).isFalse();
    }

    @Test
    void cannotSetUpOrEnableTwice() {
        turnOn();

        assertThatThrownBy(() -> service.setup("luisa@bg.es")).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> service.enable("luisa@bg.es", "123456")).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void enableWithoutSetupIsRejected() {
        assertThatThrownBy(() -> service.enable("luisa@bg.es", "123456")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCodeOfTheAppWorksOnlyOnce() {
        turnOn();
        String next = codeNow(1);

        assertThat(service.verify(admin, next, Instant.now())).isTrue();
        assertThat(service.verify(admin, next, Instant.now())).isFalse();
    }

    @Test
    void theCodeUsedToTurnItOnCannotBeUsedAgain() {
        service.setup("luisa@bg.es");
        String first = codeNow(0);
        service.enable("luisa@bg.es", first);

        assertThat(service.verify(admin, first, Instant.now())).isFalse();
    }

    @Test
    void aRecoveryCodeWorksOnlyOnceWithOrWithoutTheDash() {
        List<String> codes = turnOn();

        assertThat(service.verify(admin, codes.get(0).replace("-", "").toUpperCase(), Instant.now())).isTrue();
        assertThat(service.verify(admin, codes.get(0), Instant.now())).isFalse();
        assertThat(service.verify(admin, codes.get(1), Instant.now())).isTrue();
    }

    @Test
    void nothingIsValidWhileItIsOff() {
        service.setup("luisa@bg.es");

        assertThat(service.verify(admin, codeNow(0), Instant.now())).isFalse();
    }

    @Test
    void statusSaysIfItIsOnAndHowManyRecoveryCodesAreLeft() {
        assertThat(service.status("luisa@bg.es")).isEqualTo(new TwoFactorService.Status(false, 0));

        turnOn();
        when(recoveryCodeRepository.countByUserAndUsedAtIsNull(admin)).thenReturn(9L);

        assertThat(service.status("luisa@bg.es")).isEqualTo(new TwoFactorService.Status(true, 9));
    }

    @Test
    void disableForgetsTheSecretAndTheRecoveryCodes() {
        turnOn();

        service.disable("luisa@bg.es");

        assertThat(admin.isTotpEnabled()).isFalse();
        assertThat(admin.getTotpSecret()).isNull();
        verify(recoveryCodeRepository, org.mockito.Mockito.times(2)).deleteByUser(admin);
    }
}
