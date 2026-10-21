package com.bgsistemas.api.auth;

import com.bgsistemas.api.auth.dtos.LoginResponseDTO;
import com.bgsistemas.api.auth.dtos.RegisterResponseDTO;
import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private static final String REGISTER_JSON = """
            { "email": "new@mail.com", "password": "password123" }
            """;

    @Test
    void shouldReturnTokenWhenAuthenticated() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponseDTO("jwt-token", "admin@mail.com", "ADMIN"));

        mockMvc.perform(post("/api/auth/login").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // BGSI-276: with 2FA on, 202 and no JWT until the code is right
    @Test
    void shouldAskForTheCodeWhenTwoFactorIsOn() throws Exception {
        when(authService.login(any())).thenReturn(LoginResponseDTO.twoFactorPending("admin@mail.com", "pending-token"));

        mockMvc.perform(post("/api/auth/login").with(user(admin)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.twoFactorRequired").value(true))
                .andExpect(jsonPath("$.pendingToken").value("pending-token"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldReturn401OnLoginWithoutCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegisterNewAdminWhenCallerIsAdmin() throws Exception {
        when(authService.register(any())).thenReturn(new RegisterResponseDTO(2L, "new@mail.com", "ADMIN"));

        mockMvc.perform(post("/api/auth/register").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@mail.com"));
    }

    @Test
    void shouldReturn401OnRegisterWithoutSession() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).register(any());
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("new@mail.com"));

        mockMvc.perform(post("/api/auth/register").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"new@mail.com\", \"password\": \"123\" }"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }
}
