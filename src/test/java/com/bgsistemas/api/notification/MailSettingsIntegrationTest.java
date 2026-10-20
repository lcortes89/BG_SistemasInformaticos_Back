package com.bgsistemas.api.notification;

import com.bgsistemas.api.security.ConfirmationInterceptor;
import com.bgsistemas.api.security.ConfirmationTokenService;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-260/262: whole application + H2. The admin changes the recipient in the panel
// and the next contact of the website is emailed to the new address
@SpringBootTest(properties = {"app.mail.to=env@test.com", "app.mail.from=web@test.com"})
@AutoConfigureMockMvc
@DirtiesContext
class MailSettingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConfirmationTokenService confirmationTokens;

    @MockitoBean
    private JavaMailSender mailSender;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    @Test
    void theNewContactGoesToTheEmailChosenInThePanel() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        // Nothing saved yet -> the value of .env
        mockMvc.perform(get("/api/admin/settings/mail").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifyEmail").value("env@test.com"))
                .andExpect(jsonPath("$.senderAccount").value("web@test.com"));

        // BGSI-275: changing the recipient needs the password typed again
        String confirmToken = confirmationTokens.issue("admin@mail.com", Instant.now());
        mockMvc.perform(put("/api/admin/settings/mail").with(user(admin))
                        .header(ConfirmationInterceptor.HEADER, confirmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"notifyEmail\": \"duenio@test.com\", \"senderName\": \"Web de BG\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifyEmail").value("duenio@test.com"));

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Ana López", "email": "ana@mail.com", "phone": "600123123",
                                  "interestedService": "BDP One", "message": "Quiero una demo", "gdprConsent": true }
                                """))
                .andExpect(status().isCreated());

        // The email goes in the background (@Async): wait up to 2 seconds for it
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(2000)).send(sent.capture());
        assertThat(sent.getValue().getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("duenio@test.com");
    }
}
