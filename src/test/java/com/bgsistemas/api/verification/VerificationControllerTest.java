package com.bgsistemas.api.verification;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.verification.exceptions.InvalidVerificationTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-238
@Import(SecurityConfig.class)
@WebMvcTest(VerificationController.class)
class VerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VerificationService verificationService;

    @Test
    void theLinkWorksWithoutASession() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"token\": \"tok\" }"))
                .andExpect(status().isNoContent());

        verify(verificationService).verifyEmail("tok");
    }

    @Test
    void aBadOrExpiredLinkIs400() throws Exception {
        doThrow(new InvalidVerificationTokenException()).when(verificationService).verifyEmail("old");

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"token\": \"old\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aMissingTokenIs400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"token\": \"\" }"))
                .andExpect(status().isBadRequest());
    }
}
