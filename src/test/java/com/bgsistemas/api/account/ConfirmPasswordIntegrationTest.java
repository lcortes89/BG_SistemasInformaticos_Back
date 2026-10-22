package com.bgsistemas.api.account;

import com.bgsistemas.api.security.ConfirmationInterceptor;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-275: whole flow with the H2 database. Sensitive actions need the password typed again
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class ConfirmPasswordIntegrationTest {

    private static final String CONFIRM_URL = "/api/admin/account/confirm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SecurityUser saveAdmin(String email) {
        return new SecurityUser(userRepository.save(new User(email, passwordEncoder.encode("Correct2026"), Role.ADMIN)));
    }

    private ResultActions confirm(SecurityUser admin, String password) throws Exception {
        return mockMvc.perform(post(CONFIRM_URL).with(user(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"" + password + "\" }"));
    }

    private String confirmToken(SecurityUser admin) throws Exception {
        String json = confirm(admin, "Correct2026").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.confirmToken");
    }

    static Stream<Arguments> sensitiveActions() {
        return Stream.of(
                Arguments.of("delete brand", delete("/api/admin/brands/999")),
                Arguments.of("delete product", delete("/api/admin/services/999")),
                Arguments.of("delete client", delete("/api/admin/clients/999")),
                Arguments.of("delete contact", delete("/api/admin/contacts/999")),
                Arguments.of("delete rate question", delete("/api/admin/rate-questions/999")),
                Arguments.of("delete rate option", delete("/api/admin/rate-questions/options/999")),
                Arguments.of("change the contact email", put("/api/admin/settings/mail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"notifyEmail\": \"other@test.com\", \"senderName\": \"Web\" }")),
                Arguments.of("create an admin", post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"new@test.com\", \"password\": \"NewAdmin2026\" }")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sensitiveActions")
    void sensitiveActionsWithoutTheConfirmationGet403(String name, MockHttpServletRequestBuilder action) throws Exception {
        SecurityUser admin = saveAdmin("no-confirm-" + name.replace(' ', '-') + "@test.com");

        mockMvc.perform(action.with(user(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.details.reason").value("CONFIRMATION_REQUIRED"));
    }

    @Test
    void withThePasswordConfirmedTheActionsAreDoneDuringFiveMinutes() throws Exception {
        SecurityUser admin = saveAdmin("confirmed@test.com");

        confirm(admin, "Correct2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(300));
        String token = confirmToken(admin);

        // The guard lets them through: 404 because brand 999 does not exist
        mockMvc.perform(delete("/api/admin/brands/999").with(user(admin)).header(ConfirmationInterceptor.HEADER, token))
                .andExpect(status().isNotFound());
        // A second sensitive action with the same token, without typing the password again
        mockMvc.perform(delete("/api/admin/clients/999").with(user(admin)).header(ConfirmationInterceptor.HEADER, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void theTokenOfAnotherAdminDoesNotWork() throws Exception {
        SecurityUser owner = saveAdmin("owner@test.com");
        SecurityUser other = saveAdmin("other@test.com");
        String token = confirmToken(owner);

        mockMvc.perform(delete("/api/admin/brands/999").with(user(other)).header(ConfirmationInterceptor.HEADER, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void aMadeUpTokenDoesNotWork() throws Exception {
        SecurityUser admin = saveAdmin("made-up@test.com");

        mockMvc.perform(delete("/api/admin/brands/999").with(user(admin)).header(ConfirmationInterceptor.HEADER, "made-up"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aWrongPasswordGivesNoToken() throws Exception {
        SecurityUser admin = saveAdmin("wrong@test.com");

        confirm(admin, "Wrong2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").value("Password is not correct"))
                .andExpect(jsonPath("$.confirmToken").doesNotExist());
    }

    @Test
    void fiveWrongPasswordsLockTheConfirmationAndTheLogin() throws Exception {
        SecurityUser admin = saveAdmin("guess@test.com");

        for (int i = 0; i < 5; i++) {
            confirm(admin, "Wrong2026").andExpect(status().isBadRequest());
        }

        confirm(admin, "Correct2026")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.details.retryAfterMinutes").value("15"));
    }
}
