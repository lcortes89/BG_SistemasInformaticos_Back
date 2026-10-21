package com.bgsistemas.api.adminuser;

import com.bgsistemas.api.adminuser.dtos.AdminUserDTO;
import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import com.bgsistemas.api.globals.ResourceConflictException;
import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.passwordreset.PasswordResetService;
import com.bgsistemas.api.passwordreset.PasswordResetTokenRepository;
import com.bgsistemas.api.twofactor.RecoveryCodeRepository;
import com.bgsistemas.api.twofactor.TwoFactorService;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.bgsistemas.api.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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

// BGSI-277: invite, block, unblock, change email, reset 2FA and delete admins
class AdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordResetService passwordResetService = mock(PasswordResetService.class);
    private final PasswordResetTokenRepository resetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final RecoveryCodeRepository recoveryCodeRepository = mock(RecoveryCodeRepository.class);
    private final TwoFactorService twoFactorService = mock(TwoFactorService.class);
    private final PasswordResetEmailSender emailSender = mock(PasswordResetEmailSender.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AdminUserService service = new AdminUserService(userRepository, passwordResetService,
            resetTokenRepository, recoveryCodeRepository, twoFactorService, emailSender, passwordEncoder);

    private final User me = admin(1L, "luisa@bg.es");
    private final User other = admin(2L, "maria@bg.es");

    private static User admin(Long id, String email) {
        User user = new User(email, "hash", Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @BeforeEach
    void setUp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(other, me));
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        when(userRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @SuppressWarnings("unchecked")
    private List<String> noticeRecipients() {
        ArgumentCaptor<List<String>> to = ArgumentCaptor.forClass(List.class);
        verify(emailSender).sendAdminNotice(to.capture(), anyString());
        return to.getValue();
    }

    @Test
    void listsTheAdminsByEmailAndMarksTheLoggedInOne() {
        List<AdminUserDTO> list = service.list("luisa@bg.es");

        assertThat(list).extracting(AdminUserDTO::email).containsExactly("luisa@bg.es", "maria@bg.es");
        assertThat(list.get(0).you()).isTrue();
        assertThat(list.get(1).you()).isFalse();
        assertThat(list.get(1).status()).isEqualTo("ACTIVE");
    }

    @Test
    void inviteCreatesAPendingAdminAndEmailsTheLink() {
        AdminUserDTO invited = service.invite(" Nuevo@BG.es ", "luisa@bg.es");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("nuevo@bg.es");
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getValue().getPassword()).isEqualTo("random-hash");
        verify(passwordResetService).invite(saved.getValue(), "luisa@bg.es");
        assertThat(invited.status()).isEqualTo("PENDING");
        assertThat(noticeRecipients()).containsExactlyInAnyOrder("maria@bg.es", "luisa@bg.es");
    }

    @Test
    void cannotInviteAnEmailThatAlreadyExists() {
        when(userRepository.existsByEmail("maria@bg.es")).thenReturn(true);

        assertThatThrownBy(() -> service.invite("maria@bg.es", "luisa@bg.es")).isInstanceOf(EmailAlreadyExistsException.class);
        verify(passwordResetService, never()).invite(any(), anyString());
    }

    @Test
    void nobodyCanActOnTheirOwnAccountHere() {
        assertThatThrownBy(() -> service.block(1L, "luisa@bg.es")).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> service.delete(1L, "LUISA@bg.es")).isInstanceOf(ResourceConflictException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void blockAndUnblock() {
        assertThat(service.block(2L, "luisa@bg.es").status()).isEqualTo("BLOCKED");
        assertThat(other.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThatThrownBy(() -> service.block(2L, "luisa@bg.es")).isInstanceOf(ResourceConflictException.class);

        assertThat(service.unblock(2L, "luisa@bg.es").status()).isEqualTo("ACTIVE");
        assertThatThrownBy(() -> service.unblock(2L, "luisa@bg.es")).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void aBlockedAdminDoesNotReceiveTheNotices() {
        service.block(2L, "luisa@bg.es");

        assertThat(noticeRecipients()).containsExactly("luisa@bg.es");
    }

    @Test
    void theInvitationCanBeSentAgainOnlyWhilePending() {
        assertThatThrownBy(() -> service.resendInvitation(2L, "luisa@bg.es")).isInstanceOf(ResourceConflictException.class);

        other.setStatus(UserStatus.PENDING);
        service.resendInvitation(2L, "luisa@bg.es");

        verify(passwordResetService).invite(other, "luisa@bg.es");
    }

    @Test
    void changingTheEmailClosesTheSessionsAndTellsBothAddresses() {
        AdminUserDTO changed = service.changeEmail(2L, " Maria.Nueva@bg.es ", "luisa@bg.es");

        assertThat(changed.email()).isEqualTo("maria.nueva@bg.es");
        assertThat(other.getPasswordChangedAt()).isNotNull();
        assertThat(noticeRecipients()).contains("maria@bg.es", "maria.nueva@bg.es", "luisa@bg.es");
    }

    @Test
    void cannotChangeToAnEmailThatAlreadyExists() {
        when(userRepository.existsByEmail("luisa@bg.es")).thenReturn(true);

        assertThatThrownBy(() -> service.changeEmail(2L, "luisa@bg.es", "luisa@bg.es"))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void resetTwoFactorTurnsItOffForThatAdmin() {
        service.resetTwoFactor(2L, "luisa@bg.es");

        verify(twoFactorService).disable("maria@bg.es");
    }

    @Test
    void deleteRemovesTheCodesTheLinksAndTheAdmin() {
        service.delete(2L, "luisa@bg.es");

        verify(recoveryCodeRepository).deleteByUser(other);
        verify(resetTokenRepository).deleteByUser(other);
        verify(userRepository).delete(other);
        verify(emailSender).sendAdminNotice(any(), eq("luisa@bg.es ha borrado la cuenta de administrador maria@bg.es."));
    }
}
