package com.bgsistemas.api.account;

import com.bgsistemas.api.account.exceptions.InvalidPasswordChangeException;
import com.bgsistemas.api.auth.JwtService;
import com.bgsistemas.api.globals.ResourceNotFoundException;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// BGSI-272: change the password of the logged-in admin
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    private static final String EMAIL = "admin@mail.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private final User admin = new User(EMAIL, "old-hash", Role.ADMIN);

    @Test
    void changesThePasswordAndReturnsANewToken() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("OldPass2026", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("NewPass2026", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPass2026")).thenReturn("new-hash");
        when(jwtService.generateToken(admin)).thenReturn("new-token");
        Instant before = Instant.now().minusSeconds(1);

        String token = accountService.changePassword(EMAIL, "OldPass2026", "NewPass2026");

        assertThat(token).isEqualTo("new-token");
        assertThat(admin.getPassword()).isEqualTo("new-hash");
        assertThat(admin.getPasswordChangedAt()).isAfter(before);
        verify(userRepository).save(admin);
    }

    @Test
    void rejectsAWrongCurrentPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> accountService.changePassword(EMAIL, "wrong", "NewPass2026"))
                .isInstanceOf(InvalidPasswordChangeException.class)
                .hasMessage("Current password is not correct")
                .extracting("field").isEqualTo("currentPassword");
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsANewPasswordEqualToTheCurrentOne() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("OldPass2026", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> accountService.changePassword(EMAIL, "OldPass2026", "OldPass2026"))
                .isInstanceOf(InvalidPasswordChangeException.class)
                .extracting("field").isEqualTo("newPassword");
        verify(userRepository, never()).save(any());
    }

    @Test
    void failsWhenTheUserDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.changePassword(EMAIL, "OldPass2026", "NewPass2026"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
