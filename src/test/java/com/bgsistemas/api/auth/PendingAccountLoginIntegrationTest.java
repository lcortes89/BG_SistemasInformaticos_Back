package com.bgsistemas.api.auth;

import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.bgsistemas.api.user.UserStatus;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-238: only who knows the password learns that the account is waiting for the email
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class PendingAccountLoginIntegrationTest {

    private static final String GENERIC_401 = "You must log in to access this resource";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(httpBasic(email, password)));
    }

    private void saveAdmin(String email, UserStatus status) {
        User user = new User(email, passwordEncoder.encode("Correct2026"), Role.ADMIN);
        user.setStatus(status);
        userRepository.save(user);
    }

    @Test
    void aPendingAdminWithTheRightPasswordIsToldToVerifyTheEmail() throws Exception {
        saveAdmin("pending@test.com", UserStatus.PENDING);

        login("pending@test.com", "Correct2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your account is not verified yet. Check your email"))
                .andExpect(jsonPath("$.details.reason").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void aPendingAdminWithAWrongPasswordGetsTheGenericAnswer() throws Exception {
        saveAdmin("pending-wrong@test.com", UserStatus.PENDING);

        login("pending-wrong@test.com", "Wrong2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_401))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void aBlockedAdminGetsTheGenericAnswerEvenWithTheRightPassword() throws Exception {
        saveAdmin("blocked@test.com", UserStatus.BLOCKED);

        login("blocked@test.com", "Correct2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GENERIC_401))
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}
