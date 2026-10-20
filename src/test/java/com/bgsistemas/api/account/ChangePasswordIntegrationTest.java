package com.bgsistemas.api.account;

import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-272: whole application with the real login and JWT
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class ChangePasswordIntegrationTest {

    private static final String EMAIL = "owner@test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String login(String password) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").with(httpBasic(EMAIL, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    @Test
    void changingThePasswordClosesTheOldSessionsAndOnlyTheNewPasswordWorks() throws Exception {
        userRepository.save(new User(EMAIL, passwordEncoder.encode("OldPass2026"), Role.ADMIN));
        String oldToken = login("OldPass2026");

        // The JWT "iat" has whole seconds: wait so the change happens in a later second
        Thread.sleep(1100);

        String json = mockMvc.perform(put("/api/admin/account/password")
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"currentPassword\": \"OldPass2026\", \"newPassword\": \"NewPass2026\" }"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newToken = JsonPath.read(json, "$.token");

        // The old session (e.g. another device) stops working, the new token works
        mockMvc.perform(get("/api/admin/summary").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/summary").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());

        // Login: the old password is rejected and the new one is accepted
        mockMvc.perform(post("/api/auth/login").with(httpBasic(EMAIL, "OldPass2026")))
                .andExpect(status().isUnauthorized());
        login("NewPass2026");
    }
}
