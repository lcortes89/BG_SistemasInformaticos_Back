package com.bgsistemas.api.verification;

import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.security.ConfirmationInterceptor;
import com.bgsistemas.api.security.ConfirmationTokenService;
import com.bgsistemas.api.security.SecurityUser;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-238: whole flow with H2. Register -> pending -> open the link -> log in.
// Each test uses its own emails: the H2 database is shared by the tests of this class
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class VerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ConfirmationTokenService confirmationTokens;

    @MockitoBean
    private PasswordResetEmailSender emailSender;

    // An existing admin registers newAdmin; returns the token that would go in the email
    private String registerAndGetTheLinkToken(String ownerEmail, String newAdmin) throws Exception {
        SecurityUser owner = new SecurityUser(userRepository.save(
                new User(ownerEmail, passwordEncoder.encode("Correct2026"), Role.ADMIN)));
        String confirmToken = confirmationTokens.issue(owner.getUsername(), Instant.now());

        mockMvc.perform(post("/api/auth/register").with(user(owner))
                        .header(ConfirmationInterceptor.HEADER, confirmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"" + newAdmin + "\", \"password\": \"Correct2026\" }"))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmailVerification(eq(newAdmin), token.capture());
        return token.getValue();
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(httpBasic(email, "Correct2026")));
    }

    private ResultActions postToken(String url, String token) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"token\": \"" + token + "\" }"));
    }

    @Test
    void theNewAdminCanOnlyLogInAfterOpeningTheLinkAndTheLinkWorksOnce() throws Exception {
        String newAdmin = "verify-new@test.com";
        String token = registerAndGetTheLinkToken("verify-owner@test.com", newAdmin);

        login(newAdmin).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.details.reason").value("EMAIL_NOT_VERIFIED"));

        postToken("/api/auth/verify-email", token).andExpect(status().isNoContent());
        login(newAdmin).andExpect(status().isOk());

        postToken("/api/auth/verify-email", token).andExpect(status().isBadRequest());
    }

    @Test
    void aPendingAdminCanAskForANewLinkWithTheOldOne() throws Exception {
        String newAdmin = "resend-new@test.com";
        String token = registerAndGetTheLinkToken("resend-owner@test.com", newAdmin);

        postToken("/api/auth/verify-email/resend", token).andExpect(status().isAccepted());

        verify(emailSender, times(2)).sendEmailVerification(eq(newAdmin), anyString());
    }
}
