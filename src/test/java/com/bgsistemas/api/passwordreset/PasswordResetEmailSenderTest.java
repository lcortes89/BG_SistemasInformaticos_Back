package com.bgsistemas.api.passwordreset;

import com.bgsistemas.api.notification.MailSettings;
import com.bgsistemas.api.notification.MailSettingsService;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// BGSI-273: the JavaMailSender is a mock, no real email is sent
@ExtendWith(MockitoExtension.class)
class PasswordResetEmailSenderTest {

    private static final String URL = "http://localhost:5173/admin/nueva-contrasena";

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailSettingsService mailSettingsService;

    private final MailSettings settings = new MailSettings("negocio@bgsistemas.es", "BG Sistemas Web", "web@bgsistemas.es");

    private PasswordResetEmailSender sender() {
        return new PasswordResetEmailSender(mailSenderProvider, mailSettingsService, URL);
    }

    @Test
    void sendsTheLinkToTheAdmin() throws Exception {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSettingsService.getSettings()).thenReturn(settings);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        sender().send("admin@mail.com", "abc123");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("admin@mail.com");
        assertThat(sent.getValue().getSubject()).isEqualTo(PasswordResetEmailSender.SUBJECT);
    }

    @Test
    void sendsTheSecurityAlertToEveryAdmin() throws Exception {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSettingsService.getSettings()).thenReturn(settings);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        UnknownEmailAlarm.Alert alert = new UnknownEmailAlarm.Alert(12, List.of("nobody@mail.com", "luisa@hotmal.com"));

        sender().sendUnknownEmailAlert(List.of("admin@mail.com", "other@mail.com"), alert);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getRecipients(Message.RecipientType.TO)).hasSize(2);
        assertThat(sent.getValue().getSubject()).isEqualTo(PasswordResetEmailSender.ALERT_SUBJECT);
        assertThat(PasswordResetEmailSender.alertText(alert)).contains("12 veces").contains("- luisa@hotmal.com");
        assertThat(PasswordResetEmailSender.alertHtml(alert)).contains("<strong>12 veces</strong>")
                .contains("<li>nobody@mail.com</li>").doesNotContain("href=");
    }

    @Test
    void escapesTheEmailsTypedByTheVisitor() {
        UnknownEmailAlarm.Alert alert = new UnknownEmailAlarm.Alert(10, List.of("<b>x</b>@mail.com"));

        assertThat(PasswordResetEmailSender.alertHtml(alert)).contains("&lt;b&gt;x&lt;/b&gt;@mail.com")
                .doesNotContain("<b>x</b>");
    }

    @Test
    void theEmailContainsTheLinkWithTheToken() {
        String link = sender().link("abc123");

        assertThat(link).isEqualTo(URL + "?token=abc123");
        assertThat(PasswordResetEmailSender.text(link)).contains(link).contains("30 minutos");
        assertThat(PasswordResetEmailSender.html(link)).contains("href=\"" + link + "\"");
    }

    @Test
    void doesNothingWhenEmailIsNotConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        when(mailSettingsService.getSettings()).thenReturn(settings);

        assertThatCode(() -> sender().send("admin@mail.com", "abc123")).doesNotThrowAnyException();
    }

    @Test
    void doesNotThrowWhenGmailFails() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSettingsService.getSettings()).thenReturn(settings);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("Gmail is down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> sender().send("admin@mail.com", "abc123")).doesNotThrowAnyException();
    }

    @Test
    void doesNotSendWithoutASenderAccount() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSettingsService.getSettings()).thenReturn(new MailSettings("x@mail.com", "BG", ""));

        sender().send("admin@mail.com", "abc123");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
