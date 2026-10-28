package com.bgsistemas.api.notification;

import com.bgsistemas.api.contact.Contact;
import com.bgsistemas.api.notification.exceptions.EmailNotSentException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

// BGSI-120: sends the "new contact" email to the business with Gmail
@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final ClassPathResource LOGO = new ClassPathResource("pdf/logo.png");
    static final String TEST_SUBJECT = "Correo de prueba · así verás los nuevos contactos";

    // Optional: without spring.mail.host (e.g. in the tests) there is no JavaMailSender
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ContactEmailTemplate template;
    private final MailSettingsService mailSettingsService;

    public EmailNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    ContactEmailTemplate template,
                                    MailSettingsService mailSettingsService) {
        this.mailSenderProvider = mailSenderProvider;
        this.template = template;
        this.mailSettingsService = mailSettingsService;
    }

    // @Async: the visitor gets the answer at once, the email is sent in the background
    @Async
    @Override
    public void notifyNewContact(Contact contact) {
        try {
            send(template.subject(contact), contact);
        } catch (EmailNotSentException e) {
            // BGSI-121: the contact is already saved, a failed email must not break the form
            log.warn("Could not send the new contact email: {}", e.getMessage());
        }
    }

    // Not @Async: the admin waits to know if the email arrived
    @Override
    public void sendTest() {
        Contact example = new Contact("Cliente de ejemplo", "cliente@ejemplo.com", "600 000 000", "BDP One",
                "Este es un correo de prueba enviado desde el panel.\nAsí verás cada nuevo contacto de la web.", true);
        send(TEST_SUBJECT, example);
    }

    private void send(String subject, Contact contact) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        MailSettings settings = mailSettingsService.getSettings();
        if (mailSender == null || !settings.canSend()) {
            throw new EmailNotSentException("Email is not configured: set MAIL_USERNAME, MAIL_PASSWORD and MAIL_TO in .env");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(settings.senderAccount(), settings.senderName());
            helper.setTo(settings.notifyEmail());
            helper.setReplyTo(contact.getEmail()); // "Reply" writes straight to the visitor
            helper.setSubject(subject);
            helper.setText(template.text(contact), template.html(contact));
            helper.addInline("logo", LOGO); // the <img src="cid:logo"> of the HTML
            mailSender.send(message);
            log.info("Email \"{}\" sent to {}", subject, settings.notifyEmail());
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new EmailNotSentException("The email could not be sent: " + e.getMessage(), e);
        }
    }
}
