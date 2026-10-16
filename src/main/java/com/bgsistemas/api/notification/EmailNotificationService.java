package com.bgsistemas.api.notification;

import com.bgsistemas.api.contact.Contact;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;

// BGSI-120: sends the "new contact" email to the business with Gmail
@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final ClassPathResource LOGO = new ClassPathResource("pdf/logo.png");

    // Optional: without spring.mail.host (e.g. in the tests) there is no JavaMailSender
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ContactEmailTemplate template;
    private final String to;
    private final String from;

    public EmailNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    ContactEmailTemplate template,
                                    @Value("${app.mail.to:}") String to,
                                    @Value("${app.mail.from:}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.template = template;
        this.to = to;
        this.from = from;
    }

    // @Async: the visitor gets the answer at once, the email is sent in the background
    @Async
    @Override
    public void notifyNewContact(Contact contact) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(to) || !StringUtils.hasText(from)) {
            log.info("Email notifications are off: set MAIL_USERNAME, MAIL_PASSWORD and MAIL_TO in .env");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, "BG Sistemas Web");
            helper.setTo(to);
            helper.setReplyTo(contact.getEmail()); // "Reply" writes straight to the visitor
            helper.setSubject(template.subject(contact));
            helper.setText(template.text(contact), template.html(contact));
            helper.addInline("logo", LOGO); // the <img src="cid:logo"> of the HTML
            mailSender.send(message);
            log.info("New contact email sent to {}", to);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            // BGSI-121: the contact is already saved, a failed email must not break the form
            log.warn("Could not send the new contact email: {}", e.getMessage());
        }
    }
}
