package com.bgsistemas.api.account;

import com.bgsistemas.api.account.exceptions.InvalidPasswordChangeException;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-272: PUT /api/admin/account/password
@Import(SecurityConfig.class)
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final String URL = "/api/admin/account/password";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private String body(String current, String next) {
        return "{ \"currentPassword\": \"" + current + "\", \"newPassword\": \"" + next + "\" }";
    }

    @Test
    void returnsANewTokenForTheLoggedInAdmin() throws Exception {
        when(accountService.changePassword("admin@mail.com", "OldPass2026", "NewPass2026")).thenReturn("new-token");

        mockMvc.perform(put(URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body("OldPass2026", "NewPass2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-token"));
    }

    @Test
    void needsToBeLoggedIn() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(body("OldPass2026", "NewPass2026")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAWeakNewPassword() throws Exception {
        mockMvc.perform(put(URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body("OldPass2026", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.newPassword").exists());
        verify(accountService, never()).changePassword(anyString(), anyString(), anyString());
    }

    @Test
    void rejectsANewPasswordWithoutUppercaseOrNumber() throws Exception {
        mockMvc.perform(put(URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body("OldPass2026", "onlylowercase")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.newPassword")
                        .value("New password needs an uppercase letter, a lowercase letter and a number"));
    }

    @Test
    void tellsWhichFieldIsWrongWhenTheCurrentPasswordFails() throws Exception {
        when(accountService.changePassword("admin@mail.com", "WrongPass2026", "NewPass2026"))
                .thenThrow(new InvalidPasswordChangeException("currentPassword", "Current password is not correct"));

        mockMvc.perform(put(URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body("WrongPass2026", "NewPass2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.currentPassword").value("Current password is not correct"));
    }
}
