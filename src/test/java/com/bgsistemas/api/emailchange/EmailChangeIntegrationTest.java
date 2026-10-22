package com.bgsistemas.api.emailchange;

import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.security.ConfirmationInterceptor;
import com.bgsistemas.api.security.ConfirmationTokenService;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-299: whole flow with H2. Ask for the change -> open the link -> log in with the new email
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class EmailChangeIntegrationTest {

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

    private SecurityUser saveAdmin(String email) {
        return new SecurityUser(userRepository.save(new User(email, passwordEncoder.encode("Correct2026"), Role.ADMIN)));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(httpBasic(email, "Correct2026")));
    }

    private ResultActions askForChange(SecurityUser admin, String newEmail) throws Exception {
        String confirmToken = confirmationTokens.issue(admin.getUsername(), Instant.now());
        return mockMvc.perform(post("/api/admin/account/email").with(user(admin))
                .header(ConfirmationInterceptor.HEADER, confirmToken)
                .contentType(MediaType.APPLICATION_JSON).content("{ \"newEmail\": \"" + newEmail + "\" }"));
    }

    private ResultActions openLink(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/confirm-email").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"token\": \"" + token + "\" }"));
    }

    @Test
    void theEmailChangesOnlyWhenTheLinkIsOpened() throws Exception {
        SecurityUser luisa = saveAdmin("luisa-change@test.com");
        String jwt = JsonPath.read(login("luisa-change@test.com").andReturn().getResponse().getContentAsString(), "$.token");

        askForChange(luisa, "Luisa-New@Test.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.newEmail").value("luisa-new@test.com"));
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmailChangeLink(eq("luisa-new@test.com"), token.capture(), eq("luisa-change@test.com"));
        verify(emailSender).sendEmailChangeRequested("luisa-change@test.com", "luisa-new@test.com");

        // Before the link: same email, and the change is shown as pending
        login("luisa-change@test.com").andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/account/email").with(user(luisa)))
                .andExpect(jsonPath("$.newEmail").value("luisa-new@test.com"));

        openLink(token.getValue()).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("luisa-new@test.com"));

        login("luisa-new@test.com").andExpect(status().isOk());
        login("luisa-change@test.com").andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/summary").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
        openLink(token.getValue()).andExpect(status().isBadRequest()); // one use only
    }

    @Test
    void itNeedsThePasswordAndAFreeEmail() throws Exception {
        SecurityUser luisa = saveAdmin("luisa-rules@test.com");
        saveAdmin("taken@test.com");

        mockMvc.perform(post("/api/admin/account/email").with(user(luisa))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"newEmail\": \"free@test.com\" }"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.details.reason").value("CONFIRMATION_REQUIRED"));
        askForChange(luisa, "taken@test.com").andExpect(status().isConflict());
        askForChange(luisa, "luisa-rules@test.com").andExpect(status().isBadRequest());
    }

    @Test
    void aCancelledChangeCannotBeConfirmed() throws Exception {
        SecurityUser luisa = saveAdmin("luisa-cancel@test.com");
        askForChange(luisa, "luisa-cancelled@test.com").andExpect(status().isAccepted());
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmailChangeLink(eq("luisa-cancelled@test.com"), token.capture(), eq("luisa-cancel@test.com"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/account/email")
                .with(user(luisa))).andExpect(status().isNoContent());

        openLink(token.getValue()).andExpect(status().isBadRequest());
        login("luisa-cancel@test.com").andExpect(status().isOk());
    }
}
