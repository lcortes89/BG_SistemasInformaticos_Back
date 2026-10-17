package com.bgsistemas.api.notification;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.notification.exceptions.EmailNotSentException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-260: only the admin reads and changes the email settings
@Import(SecurityConfig.class)
@WebMvcTest(AdminMailSettingsController.class)
class AdminMailSettingsControllerTest {

    private static final String URL = "/api/admin/settings/mail";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MailSettingsService mailSettingsService;

    @MockitoBean
    private NotificationService notificationService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private final MailSettings settings = new MailSettings("duenio@bg.es", "BG Sistemas Web", "web@gmail.com");

    @Test
    void shouldReturn401WithoutLogin() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnTheSettingsWithTheSenderAccount() throws Exception {
        when(mailSettingsService.getSettings()).thenReturn(settings);

        mockMvc.perform(get(URL).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifyEmail").value("duenio@bg.es"))
                .andExpect(jsonPath("$.senderName").value("BG Sistemas Web"))
                .andExpect(jsonPath("$.senderAccount").value("web@gmail.com"));
    }

    @Test
    void shouldUpdateTheSettings() throws Exception {
        when(mailSettingsService.updateSettings("duenio@bg.es", "Web de BG"))
                .thenReturn(new MailSettings("duenio@bg.es", "Web de BG", "web@gmail.com"));

        mockMvc.perform(put(URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"notifyEmail\": \"duenio@bg.es\", \"senderName\": \"Web de BG\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderName").value("Web de BG"));
    }

    @Test
    void shouldReturn400WhenTheEmailIsNotValid() throws Exception {
        mockMvc.perform(put(URL).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"notifyEmail\": \"not-an-email\", \"senderName\": \"Web de BG\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.notifyEmail").exists());

        verify(mailSettingsService, never()).updateSettings(any(), any());
    }

    @Test
    void shouldSendATestEmail() throws Exception {
        mockMvc.perform(post(URL + "/test").with(user(admin)))
                .andExpect(status().isNoContent());

        verify(notificationService).sendTest();
    }

    @Test
    void shouldReturn503WhenTheTestEmailFails() throws Exception {
        doThrow(new EmailNotSentException("The email could not be sent: Authentication failed"))
                .when(notificationService).sendTest();

        mockMvc.perform(post(URL + "/test").with(user(admin)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("The email could not be sent: Authentication failed"));
    }
}
