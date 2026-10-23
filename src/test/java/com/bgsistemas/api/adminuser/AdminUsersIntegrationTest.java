package com.bgsistemas.api.adminuser;

import com.bgsistemas.api.passwordreset.PasswordResetEmailSender;
import com.bgsistemas.api.security.ConfirmationTokenService;
import com.bgsistemas.api.security.ConfirmationInterceptor;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.bgsistemas.api.user.UserStatus;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-277: whole flow with H2. Invite -> the new admin chooses the password -> block -> unblock -> delete
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class AdminUsersIntegrationTest {

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

    // A sensitive action with the password already confirmed (BGSI-275)
    private ResultActions confirmed(MockHttpServletRequestBuilder action, SecurityUser admin) throws Exception {
        String token = confirmationTokens.issue(admin.getUsername(), Instant.now());
        return mockMvc.perform(action.with(user(admin)).header(ConfirmationInterceptor.HEADER, token));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(httpBasic(email, password)));
    }

    private Long idOf(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    @Test
    void anInvitedAdminChoosesThePasswordAndCanLogIn() throws Exception {
        SecurityUser luisa = saveAdmin("luisa-invites@test.com");

        confirmed(post("/api/admin/users/invitations").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"Invited@Test.com\" }"), luisa)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("invited@test.com"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendInvitation(eq("invited@test.com"), token.capture(), eq("luisa-invites@test.com"));

        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"token\": \"" + token.getValue() + "\", \"newPassword\": \"Invited2026\" }"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail("invited@test.com").orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        login("invited@test.com", "Invited2026").andExpect(status().isOk());
        assertThat(userRepository.findByEmail("invited@test.com").orElseThrow().getLastLoginAt()).isNotNull();
    }

    @Test
    void aBlockedAdminCannotLogInNorUseTheOpenSession() throws Exception {
        SecurityUser luisa = saveAdmin("luisa-blocks@test.com");
        saveAdmin("to-block@test.com");
        String jwt = JsonPath.read(login("to-block@test.com", "Correct2026").andReturn().getResponse().getContentAsString(), "$.token");

        confirmed(put("/api/admin/users/" + idOf("to-block@test.com") + "/block"), luisa)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        login("to-block@test.com", "Correct2026").andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/summary").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
        verify(emailSender).sendAdminNotice(org.mockito.ArgumentMatchers.argThat(to -> to.contains("luisa-blocks@test.com")), anyString());

        confirmed(put("/api/admin/users/" + idOf("to-block@test.com") + "/unblock"), luisa).andExpect(status().isOk());
        login("to-block@test.com", "Correct2026").andExpect(status().isOk());
    }

    @Test
    void changesNeedThePasswordAndNeverApplyToYourself() throws Exception {
        SecurityUser luisa = saveAdmin("luisa-self@test.com");
        saveAdmin("someone@test.com");

        mockMvc.perform(put("/api/admin/users/" + idOf("someone@test.com") + "/block").with(user(luisa)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.details.reason").value("CONFIRMATION_REQUIRED"));

        confirmed(delete("/api/admin/users/" + idOf("luisa-self@test.com")), luisa).andExpect(status().isConflict());
    }

    @Test
    void anotherAdminCanBeDeleted() throws Exception {
        SecurityUser luisa = saveAdmin("luisa-deletes@test.com");
        saveAdmin("to-delete@test.com");
        Long id = idOf("to-delete@test.com");
        login("to-delete@test.com", "Correct2026").andExpect(status().isOk());

        confirmed(delete("/api/admin/users/" + id), luisa).andExpect(status().isNoContent());
        assertThat(userRepository.findById(id)).isEmpty();
        login("to-delete@test.com", "Correct2026").andExpect(status().isUnauthorized());
    }

    @Test
    void theListShowsTheStatusAndWhoYouAre() throws Exception {
        SecurityUser luisa = saveAdmin("luisa-list@test.com");

        mockMvc.perform(get("/api/admin/users").param("size", "50").with(user(luisa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.email == 'luisa-list@test.com')].you").value(true));
    }

    // BGSI-315: the list comes one page at a time, sorted by email ("000-" goes before any other email)
    @Test
    void theListComesOnePageAtATimeSortedByEmail() throws Exception {
        SecurityUser luisa = saveAdmin("000-page-c@test.com");
        saveAdmin("000-page-a@test.com");
        saveAdmin("000-page-b@test.com");

        mockMvc.perform(get("/api/admin/users").param("size", "2").with(user(luisa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].email").value("000-page-a@test.com"))
                .andExpect(jsonPath("$.content[1].email").value("000-page-b@test.com"))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/admin/users").param("size", "2").param("page", "1").with(user(luisa)))
                .andExpect(jsonPath("$.content[0].email").value("000-page-c@test.com"))
                .andExpect(jsonPath("$.content[0].you").value(true));
    }
}
