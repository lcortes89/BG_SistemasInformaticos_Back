package com.bgsistemas.api.passwordreset;

import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-273: whole flow with the H2 database. Only the email is replaced, to read the link
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class PasswordResetIntegrationTest {

    private static final String EMAIL = "forgot@test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private PasswordResetEmailSender emailSender;

    private void askForTheLink(String email) throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"" + email + "\" }"))
                .andExpect(status().isAccepted());
    }

    private String resetBody(String token, String password) {
        return "{ \"token\": \"" + token + "\", \"newPassword\": \"" + password + "\" }";
    }

    @Test
    void theAdminCreatesANewPasswordWithTheLinkOfTheEmail() throws Exception {
        userRepository.save(new User(EMAIL, passwordEncoder.encode("OldPass2026"), Role.ADMIN));

        askForTheLink(EMAIL);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq(EMAIL), token.capture());

        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody(token.getValue(), "NewPass2026")))
                .andExpect(status().isNoContent());

        // Only the new password works
        mockMvc.perform(post("/api/auth/login").with(httpBasic(EMAIL, "OldPass2026")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").with(httpBasic(EMAIL, "NewPass2026")))
                .andExpect(status().isOk());

        // The link only works once
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody(token.getValue(), "OtherPass2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The link is not valid or has expired"));
    }

    @Test
    void anUnknownEmailGetsTheSameAnswerAndNoEmailIsSent() throws Exception {
        askForTheLink("nobody@test.com");

        verify(emailSender, never()).send(anyString(), anyString());
    }
}
