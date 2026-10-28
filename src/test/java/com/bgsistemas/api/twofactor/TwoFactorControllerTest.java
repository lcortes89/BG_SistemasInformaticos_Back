package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-276: /api/admin/account/2fa
@Import(SecurityConfig.class)
@WebMvcTest(TwoFactorController.class)
class TwoFactorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TwoFactorService twoFactorService;

    private final SecurityUser admin = new SecurityUser(new User("luisa@bg.es", "hashed", Role.ADMIN));

    @Test
    void returnsTheStatus() throws Exception {
        when(twoFactorService.status("luisa@bg.es")).thenReturn(new TwoFactorService.Status(true, 7));

        mockMvc.perform(get("/api/admin/account/2fa").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.recoveryCodesLeft").value(7));
    }

    @Test
    void setupReturnsTheSecretAndTheQrText() throws Exception {
        when(twoFactorService.setup("luisa@bg.es")).thenReturn(new TwoFactorService.Setup("ABC", "otpauth://totp/x"));

        mockMvc.perform(post("/api/admin/account/2fa/setup").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("ABC"))
                .andExpect(jsonPath("$.otpauthUri").value("otpauth://totp/x"));
    }

    @Test
    void enableReturnsTheRecoveryCodes() throws Exception {
        when(twoFactorService.enable("luisa@bg.es", "123456")).thenReturn(List.of("aaaa-bbbb", "cccc-dddd"));

        mockMvc.perform(post("/api/admin/account/2fa/enable").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"code\": \"123456\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryCodes[1]").value("cccc-dddd"));
    }

    @Test
    void aWrongCodeIsA400OnTheCodeField() throws Exception {
        when(twoFactorService.enable("luisa@bg.es", "000000")).thenThrow(new InvalidCodeException());

        mockMvc.perform(post("/api/admin/account/2fa/enable").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"code\": \"000000\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.code").value("Code is not correct"));
    }

    @Test
    void needsToBeLoggedIn() throws Exception {
        mockMvc.perform(get("/api/admin/account/2fa")).andExpect(status().isUnauthorized());
    }
}
