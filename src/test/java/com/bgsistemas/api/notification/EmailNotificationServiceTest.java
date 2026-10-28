package com.bgsistemas.api.notification;

import com.bgsistemas.api.contact.Contact;
import com.bgsistemas.api.notification.exceptions.EmailNotSentException;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The JavaMailSender is a mock: no real email is sent
@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailSettingsService mailSettingsService;

    private final ContactEmailTemplate template = new ContactEmailTemplate("http://localhost:5173/admin/contactos");

    private final Contact contact = new Contact("Ana López", "ana@mail.com", "600123123", "BDP One", "Quiero una demo", true);

    private final MailSettings settings = new MailSettings("negocio@bgsistemas.es", "BG Sistemas Web", "web@bgsistemas.es");

    private EmailNotificationService service() {
        return new EmailNotificationService(mailSenderProvider, template, mailSettingsService);
    }

    private void mailIsConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSettingsService.getSettings()).thenReturn(settings);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    private MimeMessage sentMessage() {
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        return sent.getValue();
    }

    @Test
    void sendsTheEmailToTheBusinessWithReplyToTheVisitor() throws Exception {
        mailIsConfigured();

        service().notifyNewContact(contact);

        MimeMessage message = sentMessage();
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("negocio@bgsistemas.es");
        assertThat(((InternetAddress) message.getReplyTo()[0]).getAddress()).isEqualTo("ana@mail.com");
        assertThat(from.getAddress()).isEqualTo("web@bgsistemas.es");
        assertThat(from.getPersonal()).isEqualTo("BG Sistemas Web");
        assertThat(message.getSubject()).isEqualTo("Nuevo contacto: Ana López · BDP One");
    }

    // BGSI-121: Gmail is down or the password is wrong -> no exception, only a log line
    @Test
    void doesNotThrowWhenTheEmailCannotBeSent() {
        mailIsConfigured();
        doThrow(new MailSendException("Gmail is down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> service().notifyNewContact(contact)).doesNotThrowAnyException();
    }

    // BGSI-121: no recipient -> nothing is sent, and no exception either
    @Test
    void doesNothingWhenThereIsNoRecipient() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSettingsService.getSettings()).thenReturn(new MailSettings("", "BG Sistemas Web", "web@bgsistemas.es"));

        assertThatCode(() -> service().notifyNewContact(contact)).doesNotThrowAnyException();
        verify(mailSender, never()).createMimeMessage();
    }

    // BGSI-260: the test email uses the same design with an example contact
    @Test
    void sendsATestEmailWithAnExampleContact() throws Exception {
        mailIsConfigured();

        service().sendTest();

        MimeMessage message = sentMessage();
        assertThat(message.getSubject()).isEqualTo(EmailNotificationService.TEST_SUBJECT);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("negocio@bgsistemas.es");
    }

    // BGSI-260: unlike the contact email, the admin must know when the test fails
    @Test
    void theTestEmailThrowsWhenGmailRefusesIt() {
        mailIsConfigured();
        doThrow(new MailSendException("Authentication failed")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service().sendTest())
                .isInstanceOf(EmailNotSentException.class)
                .hasMessageContaining("Authentication failed");
    }

    // No spring.mail.host (e.g. in the tests) -> there is no JavaMailSender
    @Test
    void theTestEmailThrowsWhenEmailIsNotConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(mailSettingsService.getSettings()).thenReturn(settings);

        assertThatThrownBy(() -> service().sendTest())
                .isInstanceOf(EmailNotSentException.class)
                .hasMessageContaining("not configured");
    }
}
