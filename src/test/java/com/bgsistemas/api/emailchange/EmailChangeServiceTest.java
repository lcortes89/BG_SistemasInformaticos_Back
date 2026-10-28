package com.bgsistemas.api.emailchange;

import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import com.bgsistemas.api.emailchange.dtos.PendingEmailChangeDTO;
import com.bgsistemas.api.emailchange.exceptions.InvalidEmailChangeTokenException;
import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.passwordreset.PasswordResetServiceImpl;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.bgsistemas.api.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// BGSI-299: change my own email with a link sent to the new address
class EmailChangeServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailChangeTokenRepository tokenRepository = mock(EmailChangeTokenRepository.class);
    private final PasswordResetEmailSender emailSender = mock(PasswordResetEmailSender.class);
    private final EmailChangeService service = new EmailChangeService(userRepository, tokenRepository, emailSender);

    private final User luisa = new User("luisa@bg.es", "hash", Role.ADMIN);
    private final User maria = new User("maria@bg.es", "hash", Role.ADMIN);

    @BeforeEach
    void setUp() {
        when(userRepository.findByEmail("luisa@bg.es")).thenReturn(Optional.of(luisa));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(luisa, maria));
        when(tokenRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private EmailChangeToken link(String raw, String newEmail, Instant expiresAt) {
        EmailChangeToken token = new EmailChangeToken(luisa, newEmail, PasswordResetServiceImpl.hash(raw),
                Instant.now().minusSeconds(60), expiresAt);
        when(tokenRepository.findByTokenHash(PasswordResetServiceImpl.hash(raw))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void aRequestSendsTheLinkToTheNewEmailAndANoticeToTheOldOne() {
        EmailChangeToken old = link("old", "otro@bg.es", Instant.now().plusSeconds(600));
        when(tokenRepository.findByUserAndUsedAtIsNull(luisa)).thenReturn(List.of(old));

        PendingEmailChangeDTO pending = service.request("luisa@bg.es", " Luisa.Nueva@BG.es ");

        ArgumentCaptor<EmailChangeToken> saved = ArgumentCaptor.forClass(EmailChangeToken.class);
        verify(tokenRepository).save(saved.capture());
        ArgumentCaptor<String> sentToken = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmailChangeLink(eq("luisa.nueva@bg.es"), sentToken.capture(), eq("luisa@bg.es"));
        verify(emailSender).sendEmailChangeRequested("luisa@bg.es", "luisa.nueva@bg.es");

        assertThat(saved.getValue().getTokenHash()).isEqualTo(PasswordResetServiceImpl.hash(sentToken.getValue()));
        assertThat(Duration.between(saved.getValue().getCreatedAt(), saved.getValue().getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(pending.newEmail()).isEqualTo("luisa.nueva@bg.es");
        assertThat(old.getUsedAt()).isNotNull(); // only the last link works
        assertThat(luisa.getEmail()).isEqualTo("luisa@bg.es"); // nothing changes until the link is opened
    }

    @Test
    void theSameEmailOrOneThatExistsIsRejected() {
        when(userRepository.existsByEmail("maria@bg.es")).thenReturn(true);

        assertThatThrownBy(() -> service.request("luisa@bg.es", "LUISA@bg.es")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.request("luisa@bg.es", "maria@bg.es")).isInstanceOf(EmailAlreadyExistsException.class);
        verify(emailSender, never()).sendEmailChangeLink(anyString(), anyString(), anyString());
    }

    @Test
    void theLinkChangesTheEmailClosesTheSessionsAndTellsTheAdmins() {
        EmailChangeToken token = link("raw", "luisa.nueva@bg.es", Instant.now().plusSeconds(600));
        when(tokenRepository.findByUserAndUsedAtIsNull(luisa)).thenReturn(List.of(token));

        String email = service.confirm("raw");

        assertThat(email).isEqualTo("luisa.nueva@bg.es");
        assertThat(luisa.getEmail()).isEqualTo("luisa.nueva@bg.es");
        assertThat(luisa.getPasswordChangedAt()).isNotNull();
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(luisa);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> to = ArgumentCaptor.forClass(List.class);
        verify(emailSender).sendAdminNotice(to.capture(), eq("luisa@bg.es ha cambiado su email de administrador. Ahora es luisa.nueva@bg.es."));
        assertThat(to.getValue()).contains("luisa.nueva@bg.es", "maria@bg.es", "luisa@bg.es");
    }

    @Test
    void anExpiredUsedOrInventedLinkDoesNothing() {
        link("expired", "x@bg.es", Instant.now().minusSeconds(1));
        EmailChangeToken used = link("used", "y@bg.es", Instant.now().plusSeconds(600));
        used.markUsed(Instant.now());

        assertThatThrownBy(() -> service.confirm("expired")).isInstanceOf(InvalidEmailChangeTokenException.class);
        assertThatThrownBy(() -> service.confirm("used")).isInstanceOf(InvalidEmailChangeTokenException.class);
        assertThatThrownBy(() -> service.confirm("invented")).isInstanceOf(InvalidEmailChangeTokenException.class);
        assertThat(luisa.getEmail()).isEqualTo("luisa@bg.es");
    }

    @Test
    void aBlockedAdminCannotUseTheLink() {
        link("raw", "luisa.nueva@bg.es", Instant.now().plusSeconds(600));
        luisa.setStatus(UserStatus.BLOCKED);

        assertThatThrownBy(() -> service.confirm("raw")).isInstanceOf(InvalidEmailChangeTokenException.class);
    }

    @Test
    void ifAnotherAdminTookTheEmailMeanwhileItFails() {
        link("raw", "maria@bg.es", Instant.now().plusSeconds(600));
        when(userRepository.existsByEmail("maria@bg.es")).thenReturn(true);

        assertThatThrownBy(() -> service.confirm("raw")).isInstanceOf(EmailAlreadyExistsException.class);
        assertThat(luisa.getEmail()).isEqualTo("luisa@bg.es");
    }

    @Test
    void showsThePendingChangeAndCanCancelIt() {
        EmailChangeToken token = link("raw", "luisa.nueva@bg.es", Instant.now().plusSeconds(600));
        when(tokenRepository.findByUserAndUsedAtIsNull(luisa)).thenReturn(List.of(token));

        assertThat(service.pending("luisa@bg.es").newEmail()).isEqualTo("luisa.nueva@bg.es");

        service.cancel("luisa@bg.es");

        assertThat(token.getUsedAt()).isNotNull();
    }

    @Test
    void nothingPendingGivesNulls() {
        when(tokenRepository.findByUserAndUsedAtIsNull(luisa)).thenReturn(List.of());

        assertThat(service.pending("luisa@bg.es")).isEqualTo(PendingEmailChangeDTO.none());
    }
}
