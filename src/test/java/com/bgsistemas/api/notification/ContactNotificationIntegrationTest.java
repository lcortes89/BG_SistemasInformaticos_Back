package com.bgsistemas.api.notification;

import com.bgsistemas.api.contact.Contact;
import com.bgsistemas.api.contact.ContactRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-121/122: whole application + H2. Gmail fails, but the visitor's contact is saved anyway
@SpringBootTest(properties = {"app.mail.to=negocio@test.com", "app.mail.from=web@test.com"})
@AutoConfigureMockMvc
@DirtiesContext
class ContactNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void theContactIsSavedEvenWhenTheEmailCannotBeSent() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("Gmail is down")).when(mailSender).send(any(MimeMessage.class));

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Ana López", "email": "ana@mail.com", "phone": "600123123",
                                  "interestedService": "BDP One", "message": "Quiero una demo", "gdprConsent": true }
                                """))
                .andExpect(status().isCreated());

        // The email goes in the background (@Async): wait up to 2 seconds for it
        verify(mailSender, timeout(2000)).send(any(MimeMessage.class));
        assertThat(contactRepository.findAll()).extracting(Contact::getEmail).contains("ana@mail.com");
    }
}
