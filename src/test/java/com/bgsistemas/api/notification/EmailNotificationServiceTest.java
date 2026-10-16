package com.bgsistemas.api.notification;

import com.bgsistemas.api.contact.Contact;
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

    private final ContactEmailTemplate template = new ContactEmailTemplate("http://localhost:5173/admin/contactos");

    private final Contact contact = new Contact("Ana López", "ana@mail.com", "600123123", "BDP One", "Quiero una demo", true);

    private EmailNotificationService serviceSendingTo(String to) {
        return new EmailNotificationService(mailSenderProvider, template, to, "web@bgsistemas.es");
    }

    @Test
    void sendsTheEmailToTheBusinessWithReplyToTheVisitor() throws Exception {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        serviceSendingTo("negocio@bgsistemas.es").notifyNewContact(contact);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage message = sent.getValue();
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("negocio@bgsistemas.es");
        assertThat(((InternetAddress) message.getReplyTo()[0]).getAddress()).isEqualTo("ana@mail.com");
        assertThat(((InternetAddress) message.getFrom()[0]).getAddress()).isEqualTo("web@bgsistemas.es");
        assertThat(message.getSubject()).isEqualTo("Nuevo contacto: Ana López · BDP One");
    }

    // BGSI-121: Gmail is down or the password is wrong -> no exception, only a log line
    @Test
    void doesNotThrowWhenTheEmailCannotBeSent() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("Gmail is down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> serviceSendingTo("negocio@bgsistemas.es").notifyNewContact(contact))
                .doesNotThrowAnyException();
    }

    // BGSI-121: MAIL_TO is missing in .env -> nothing is sent
    @Test
    void doesNothingWhenThereIsNoRecipient() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        serviceSendingTo("").notifyNewContact(contact);

        verify(mailSender, never()).createMimeMessage();
    }

    // No spring.mail.host (e.g. in the tests) -> there is no JavaMailSender
    @Test
    void doesNothingWhenEmailIsNotConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertThatCode(() -> serviceSendingTo("negocio@bgsistemas.es").notifyNewContact(contact))
                .doesNotThrowAnyException();
    }
}
