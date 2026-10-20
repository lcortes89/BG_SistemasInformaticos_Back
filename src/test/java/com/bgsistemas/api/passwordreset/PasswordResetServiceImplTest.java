package com.bgsistemas.api.passwordreset;

import com.bgsistemas.api.passwordreset.exceptions.InvalidResetTokenException;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// BGSI-273: "¿Olvidaste tu contraseña?"
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    private static final String EMAIL = "admin@mail.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetEmailSender emailSender;

    @Mock
    private UnknownEmailAlarm unknownEmailAlarm;

    @InjectMocks
    private PasswordResetServiceImpl service;

    private final User admin = new User(EMAIL, "old-hash", Role.ADMIN);

    // ---------- request ----------

    @Test
    void sendsALinkAndOnlySavesTheHashOfTheToken() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
        when(tokenRepository.countByUserAndCreatedAtAfter(eq(admin), any())).thenReturn(0L);

        service.requestReset(" " + EMAIL + " ");

        ArgumentCaptor<String> sentToken = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(EMAIL), sentToken.capture());
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());

        PasswordResetToken token = saved.getValue();
        assertThat(token.getTokenHash()).isEqualTo(PasswordResetServiceImpl.hash(sentToken.getValue()));
        assertThat(token.getTokenHash()).isNotEqualTo(sentToken.getValue());
        assertThat(Duration.between(token.getCreatedAt(), token.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void sendsNothingToAnEmailThatIsNotAnAdmin() {
        when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());
        when(unknownEmailAlarm.record(eq(" nobody@mail.com "), any())).thenReturn(Optional.empty());

        service.requestReset(" nobody@mail.com ");

        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).send(anyString(), anyString());
        verify(emailSender, never()).sendUnknownEmailAlert(anyList(), any());
    }

    @Test
    void alertsEveryAdminWhenThereAreManyUnknownEmails() {
        UnknownEmailAlarm.Alert alert = new UnknownEmailAlarm.Alert(10, List.of("nobody@mail.com"));
        when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());
        when(unknownEmailAlarm.record(eq("nobody@mail.com"), any())).thenReturn(Optional.of(alert));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin, new User("other@mail.com", "x", Role.ADMIN)));

        service.requestReset("nobody@mail.com");

        verify(emailSender).sendUnknownEmailAlert(List.of(EMAIL, "other@mail.com"), alert);
    }

    @Test
    void doesNotAlertWhenThereAreNoAdmins() {
        UnknownEmailAlarm.Alert alert = new UnknownEmailAlarm.Alert(10, List.of("nobody@mail.com"));
        when(userRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());
        when(unknownEmailAlarm.record(eq("nobody@mail.com"), any())).thenReturn(Optional.of(alert));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of());

        service.requestReset("nobody@mail.com");

        verify(emailSender, never()).sendUnknownEmailAlert(anyList(), any());
    }

    @Test
    void stopsSendingAfterThreeRequestsInFifteenMinutes() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
        when(tokenRepository.countByUserAndCreatedAtAfter(eq(admin), any())).thenReturn(3L);

        service.requestReset(EMAIL);

        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).send(anyString(), anyString());
    }

    @Test
    void createsADifferentRandomTokenEachTime() {
        assertThat(PasswordResetServiceImpl.newToken()).isNotEqualTo(PasswordResetServiceImpl.newToken());
        assertThat(PasswordResetServiceImpl.newToken()).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    // ---------- reset ----------

    private PasswordResetToken tokenFor(String raw, Instant expiresAt) {
        return new PasswordResetToken(admin, PasswordResetServiceImpl.hash(raw), Instant.now().minusSeconds(60), expiresAt);
    }

    @Test
    void changesThePasswordAndCancelsEveryPendingLink() {
        PasswordResetToken token = tokenFor("raw-token", Instant.now().plusSeconds(600));
        PasswordResetToken otherPending = tokenFor("other-token", Instant.now().plusSeconds(600));
        when(tokenRepository.findByTokenHash(PasswordResetServiceImpl.hash("raw-token"))).thenReturn(Optional.of(token));
        when(tokenRepository.findByUserAndUsedAtIsNull(admin)).thenReturn(List.of(token, otherPending));
        when(passwordEncoder.encode("NewPass2026")).thenReturn("new-hash");

        service.resetPassword("raw-token", "NewPass2026");

        assertThat(admin.getPassword()).isEqualTo("new-hash");
        assertThat(admin.getPasswordChangedAt()).isNotNull();
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(otherPending.getUsedAt()).isNotNull();
        verify(userRepository).save(admin);
    }

    @Test
    void rejectsAnUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("invented", "NewPass2026"))
                .isInstanceOf(InvalidResetTokenException.class)
                .hasMessage("The link is not valid or has expired");
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsAnExpiredToken() {
        PasswordResetToken expired = tokenFor("raw-token", Instant.now().minusSeconds(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword("raw-token", "NewPass2026"))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void rejectsATokenThatWasAlreadyUsed() {
        PasswordResetToken used = tokenFor("raw-token", Instant.now().plusSeconds(600));
        used.markUsed(Instant.now());
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.resetPassword("raw-token", "NewPass2026"))
                .isInstanceOf(InvalidResetTokenException.class);
        verify(passwordEncoder, never()).encode(anyString());
    }
}
