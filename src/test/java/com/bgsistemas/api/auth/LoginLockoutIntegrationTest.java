package com.bgsistemas.api.auth;

import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-274: whole flow with the H2 database
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class LoginLockoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(httpBasic(email, password)));
    }

    private void saveAdmin(String email) {
        userRepository.save(new User(email, passwordEncoder.encode("Correct2026"), Role.ADMIN));
    }

    @Test
    void locksTheAccountAfterFiveWrongPasswordsEvenWithTheRightOne() throws Exception {
        saveAdmin("locked@test.com");

        for (int i = 0; i < 5; i++) {
            login("locked@test.com", "Wrong2026").andExpect(status().isUnauthorized());
        }

        login("locked@test.com", "Correct2026")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value("Too many failed attempts. Try again in 15 minutes"))
                .andExpect(jsonPath("$.details.retryAfterMinutes").value("15"));
    }

    @Test
    void aCorrectLoginBeforeTheFifthFailureStartsAgain() throws Exception {
        saveAdmin("reset@test.com");

        for (int i = 0; i < 4; i++) {
            login("reset@test.com", "Wrong2026").andExpect(status().isUnauthorized());
        }
        login("reset@test.com", "Correct2026").andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login("reset@test.com", "Wrong2026").andExpect(status().isUnauthorized());
        }

        login("reset@test.com", "Correct2026").andExpect(status().isOk());
    }

    @Test
    void anUnknownEmailIsLockedTooSoNobodyCanTellWhichEmailsExist() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("nobody@test.com", "Wrong2026").andExpect(status().isUnauthorized());
        }

        login("nobody@test.com", "Wrong2026").andExpect(status().isTooManyRequests());
    }
}
