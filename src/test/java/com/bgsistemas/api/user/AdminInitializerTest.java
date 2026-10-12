package com.bgsistemas.api.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateFirstAdminWithHashedPasswordWhenNoneExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        AdminInitializer initializer = new AdminInitializer(userRepository, passwordEncoder, "admin@mail.com", "secret");

        initializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@mail.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-secret");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldNotCreateAdminWhenOneAlreadyExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);
        AdminInitializer initializer = new AdminInitializer(userRepository, passwordEncoder, "admin@mail.com", "secret");

        initializer.run();

        verify(userRepository, never()).save(any());
    }
}
