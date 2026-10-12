package com.bgsistemas.api.auth;

import com.bgsistemas.api.auth.dtos.LoginResponseDTO;
import com.bgsistemas.api.auth.dtos.RegisterRequestDTO;
import com.bgsistemas.api.auth.dtos.RegisterResponseDTO;
import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldReturnTokenOnLogin() {
        User admin = new User("admin@mail.com", "hashed", Role.ADMIN);
        when(jwtService.generateToken(admin)).thenReturn("jwt-token");

        LoginResponseDTO response = authService.login(admin);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo("admin@mail.com");
        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    void shouldRegisterNewAdminWithHashedPassword() {
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponseDTO response = authService.register(new RegisterRequestDTO("new@mail.com", "password123"));

        assertThat(response.email()).isEqualTo("new@mail.com");
        assertThat(response.role()).isEqualTo("ADMIN");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("admin@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequestDTO("admin@mail.com", "password123")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }
}
