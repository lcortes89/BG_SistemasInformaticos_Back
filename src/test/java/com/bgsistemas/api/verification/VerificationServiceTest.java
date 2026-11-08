package com.bgsistemas.api.verification;

import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.passwordreset.PasswordResetServiceImpl;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.bgsistemas.api.user.UserStatus;
import com.bgsistemas.api.verification.exceptions.InvalidVerificationTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

// BGSI-238: the email that proves the new admin owns the address
@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    private static final String EMAIL = "new-admin@mail.com";

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private PasswordResetEmailSender emailSender;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VerificationService service;

    private final User admin = new User(EMAIL, "hashed", Role.ADMIN);

    @Test
    void sendsALinkAndOnlySavesTheHashOfTheToken() {
        Instant before = Instant.now();

        service.sendVerification(admin);

        ArgumentCaptor<String> sentToken = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmailVerification(eq(EMAIL), sentToken.capture());
        ArgumentCaptor<VerificationToken> saved = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(saved.capture());

        VerificationToken token = saved.getValue();
        assertThat(token.getUser()).isEqualTo(admin);
        assertThat(token.getTokenHash()).isEqualTo(PasswordResetServiceImpl.hash(sentToken.getValue()));
        assertThat(token.getTokenHash()).isNotEqualTo(sentToken.getValue());
        assertThat(token.getExpiresAt()).isBetween(before.plus(Duration.ofHours(24)), Instant.now().plus(Duration.ofHours(24)));
        assertThat(token.getUsedAt()).isNull();
    }

        // ---------- verify ----------

    private VerificationToken tokenFor(String token, Instant expiresAt) {
        VerificationToken saved = new VerificationToken(admin, PasswordResetServiceImpl.hash(token), expiresAt);
        when(tokenRepository.findByTokenHash(PasswordResetServiceImpl.hash(token))).thenReturn(Optional.of(saved));
        return saved;
    }

    @Test
    void aValidLinkActivatesTheAdminAndCanOnlyBeUsedOnce() {
        admin.setStatus(UserStatus.PENDING);
        VerificationToken token = tokenFor("the-token", Instant.now().plus(Duration.ofHours(1)));

        service.verifyEmail("the-token");

        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(admin);
    }

    @Test
    void anUnknownLinkIsRejected() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyEmail("wrong"))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void anExpiredLinkIsRejectedAndTheAdminStaysPending() {
        admin.setStatus(UserStatus.PENDING);
        tokenFor("old-token", Instant.now().minus(Duration.ofMinutes(1)));

        assertThatThrownBy(() -> service.verifyEmail("old-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.PENDING);
        verify(userRepository, never()).save(any());
    }

    @Test
    void aBlockedAdminIsNotActivatedByAnOldLink() {
        admin.setStatus(UserStatus.BLOCKED);
        tokenFor("the-token", Instant.now().plus(Duration.ofHours(1)));

        assertThatThrownBy(() -> service.verifyEmail("the-token"))
                .isInstanceOf(InvalidVerificationTokenException.class);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.BLOCKED);
        verify(userRepository, never()).save(any());
    }
}