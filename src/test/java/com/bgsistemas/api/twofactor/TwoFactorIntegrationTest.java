package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.security.ConfirmationInterceptor;
import com.bgsistemas.api.security.SecurityUser;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-276: whole flow with H2. Turn on 2FA, log in with the code or a recovery code, confirm and turn it off
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class TwoFactorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TotpGenerator totp;

    private SecurityUser saveAdmin(String email) {
        return new SecurityUser(userRepository.save(new User(email, passwordEncoder.encode("Correct2026"), Role.ADMIN)));
    }

    private String json(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    // The app shows this code "now" (stepsLater = 1: the next one, so it was not used yet)
    private String code(String secret, int stepsLater) {
        return totp.codeAt(secret, totp.stepAt(Instant.now()) + stepsLater);
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(httpBasic(email, "Correct2026")));
    }

    private ResultActions secondStep(String pendingToken, String code) throws Exception {
        return mockMvc.perform(post("/api/auth/login/2fa").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"pendingToken\": \"" + pendingToken + "\", \"code\": \"" + code + "\" }"));
    }

    // Turns 2FA on and returns { secret, recoveryCodes... }
    private List<String> turnOn(SecurityUser admin) throws Exception {
        String secret = JsonPath.read(json(mockMvc.perform(post("/api/admin/account/2fa/setup").with(user(admin)))
                .andExpect(status().isOk())), "$.secret");
        List<String> codes = JsonPath.read(json(mockMvc.perform(post("/api/admin/account/2fa/enable").with(user(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{ \"code\": \"" + code(secret, 0) + "\" }"))
                .andExpect(status().isOk())), "$.recoveryCodes");
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(secret), codes.stream()).toList();
    }

    @Test
    void withTwoFactorOnTheLoginNeedsTheCode() throws Exception {
        SecurityUser admin = saveAdmin("two-steps@test.com");
        String secret = turnOn(admin).get(0);

        mockMvc.perform(get("/api/admin/account/2fa").with(user(admin)))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.recoveryCodesLeft").value(10));

        String pendingToken = JsonPath.read(json(login("two-steps@test.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.twoFactorRequired").value(true))
                .andExpect(jsonPath("$.token").doesNotExist())), "$.pendingToken");

        secondStep(pendingToken, "000000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("Code is not correct"));

        secondStep(pendingToken, code(secret, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("two-steps@test.com"));

        // The pending token was used: it does not work a second time
        secondStep(pendingToken, code(secret, 1)).andExpect(status().isBadRequest());
    }

    @Test
    void aRecoveryCodeOpensTheLoginOnlyOnce() throws Exception {
        SecurityUser admin = saveAdmin("lost-phone@test.com");
        String recoveryCode = turnOn(admin).get(1);

        String first = JsonPath.read(json(login("lost-phone@test.com")), "$.pendingToken");
        secondStep(first, recoveryCode).andExpect(status().isOk());

        String second = JsonPath.read(json(login("lost-phone@test.com")), "$.pendingToken");
        secondStep(second, recoveryCode).andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/account/2fa").with(user(admin)))
                .andExpect(jsonPath("$.recoveryCodesLeft").value(9));
    }

    @Test
    void withoutTwoFactorTheLoginIsTheSameAsBefore() throws Exception {
        saveAdmin("no-2fa@test.com");

        login("no-2fa@test.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void turningItOffNeedsThePasswordAndTheCode() throws Exception {
        SecurityUser admin = saveAdmin("turn-off@test.com");
        String secret = turnOn(admin).get(0);

        mockMvc.perform(delete("/api/admin/account/2fa").with(user(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.details.reason").value("CONFIRMATION_REQUIRED"));

        mockMvc.perform(post("/api/admin/account/confirm").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"Correct2026\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("Code is required"));

        String confirmToken = JsonPath.read(json(mockMvc.perform(post("/api/admin/account/confirm").with(user(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"password\": \"Correct2026\", \"code\": \"" + code(secret, 1) + "\" }"))
                .andExpect(status().isOk())), "$.confirmToken");

        mockMvc.perform(delete("/api/admin/account/2fa").with(user(admin)).header(ConfirmationInterceptor.HEADER, confirmToken))
                .andExpect(status().isNoContent());

        login("turn-off@test.com").andExpect(status().isOk());
        assertThat(userRepository.findByEmail("turn-off@test.com").orElseThrow().getTotpSecret()).isNull();
    }

    @Test
    void theSecretIsStoredEncrypted() throws Exception {
        SecurityUser admin = saveAdmin("encrypted@test.com");
        String secret = turnOn(admin).get(0);

        String stored = userRepository.findByEmail("encrypted@test.com").orElseThrow().getTotpSecret();
        assertThat(stored).isNotBlank().doesNotContain(secret);
    }
}
