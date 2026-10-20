package com.bgsistemas.api.passwordreset;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.passwordreset.exceptions.InvalidResetTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-273: public endpoints, no login needed
@Import(SecurityConfig.class)
@WebMvcTest(PasswordResetController.class)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    void forgotPasswordAlwaysAnswers202WithoutLogin() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"admin@mail.com\" }"))
                .andExpect(status().isAccepted());

        verify(passwordResetService).requestReset("admin@mail.com");
    }

    @Test
    void forgotPasswordValidatesTheEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"not-an-email\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());

        verify(passwordResetService, never()).requestReset(anyString());
    }

    @Test
    void resetPasswordAnswers204WithoutLogin() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"abc\", \"newPassword\": \"NewPass2026\" }"))
                .andExpect(status().isNoContent());

        verify(passwordResetService).resetPassword("abc", "NewPass2026");
    }

    @Test
    void resetPasswordAppliesThePasswordRules() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"abc\", \"newPassword\": \"weak\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.newPassword").exists());

        verify(passwordResetService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPasswordAnswers400WhenTheLinkIsNotValid() throws Exception {
        doThrow(new InvalidResetTokenException()).when(passwordResetService).resetPassword("old", "NewPass2026");

        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"old\", \"newPassword\": \"NewPass2026\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The link is not valid or has expired"));
    }
}
