package com.bgsistemas.api.account;

import com.bgsistemas.api.account.exceptions.InvalidPasswordChangeException;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.TooManyAttemptsException;
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

import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-272: PUT /api/admin/account/password. BGSI-275: POST /api/admin/account/confirm
@Import(SecurityConfig.class)
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final String URL = "/api/admin/account/password";

    @Autowired
    private MockMvc mockMvc;

    private static final String CONFIRM_URL = "/api/admin/account/confirm";

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private PasswordConfirmationService passwordConfirmationService;

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

    @Test
    void confirmingThePasswordReturnsAFiveMinuteToken() throws Exception {
        when(passwordConfirmationService.confirm("admin@mail.com", "Correct2026")).thenReturn("confirm-token");

        mockMvc.perform(post(CONFIRM_URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"Correct2026\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmToken").value("confirm-token"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300));
    }

    @Test
    void confirmingNeedsAPassword() throws Exception {
        mockMvc.perform(post(CONFIRM_URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").value("Password is required"));
        verify(passwordConfirmationService, never()).confirm(anyString(), anyString());
    }

    @Test
    void aWrongPasswordWhenConfirmingIsA400OnThePasswordField() throws Exception {
        when(passwordConfirmationService.confirm("admin@mail.com", "Wrong2026"))
                .thenThrow(new InvalidPasswordChangeException("password", "Password is not correct"));

        mockMvc.perform(post(CONFIRM_URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"Wrong2026\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").value("Password is not correct"));
    }

    @Test
    void tooManyWrongPasswordsWhenConfirmingIsA429() throws Exception {
        when(passwordConfirmationService.confirm("admin@mail.com", "Wrong2026"))
                .thenThrow(new TooManyAttemptsException(Duration.ofMinutes(15)));

        mockMvc.perform(post(CONFIRM_URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"Wrong2026\" }"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.message").value("Too many failed attempts. Try again in 15 minutes"))
                .andExpect(jsonPath("$.details.retryAfterMinutes").value("15"));
    }

    @Test
    void confirmingNeedsToBeLoggedIn() throws Exception {
        mockMvc.perform(post(CONFIRM_URL)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"Correct2026\" }"))
                .andExpect(status().isUnauthorized());
    }
}
