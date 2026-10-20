package com.bgsistemas.api.passwordreset;

import com.bgsistemas.api.notification.MailSettings;
import com.bgsistemas.api.notification.MailSettingsService;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;

// BGSI-273: the "Crea una nueva contraseña" email, sent with the same Gmail account as the contact emails.
// BGSI-291: also the "there is no account with this email" notice
@Component
public class PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailSender.class);
    private static final ClassPathResource LOGO = new ClassPathResource("pdf/logo.png");
    static final String SUBJECT = "Crea una nueva contraseña · Panel de BG Sistemas";
    static final String NO_ACCOUNT_SUBJECT = "No hay ninguna cuenta con este correo · Panel de BG Sistemas";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailSettingsService mailSettingsService;
    private final String resetUrl;

    public PasswordResetEmailSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                                    MailSettingsService mailSettingsService,
                                    @Value("${app.password-reset.url:http://localhost:5173/admin/nueva-contrasena}") String resetUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailSettingsService = mailSettingsService;
        this.resetUrl = resetUrl;
    }

    String link(String token) {
        return resetUrl + "?token=" + token;
    }

    // @Async: the answer to the visitor is the same and as fast whether the email exists or not
    @Async
    public void send(String to, String token) {
        deliver(to, SUBJECT, text(link(token)), html(link(token)));
    }

    // BGSI-291: someone asked for a link with an email that is not an admin
    @Async
    public void sendNoAccount(String to) {
        deliver(to, NO_ACCOUNT_SUBJECT, noAccountText(), noAccountHtml());
    }

    private void deliver(String to, String subject, String text, String html) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        MailSettings settings = mailSettingsService.getSettings();
        if (mailSender == null || !StringUtils.hasText(settings.senderAccount())) {
            log.warn("Password reset email not sent: email is not configured (MAIL_USERNAME, MAIL_PASSWORD in .env)");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(settings.senderAccount(), settings.senderName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            helper.addInline("logo", LOGO);
            mailSender.send(message);
            log.info("Password reset email \"{}\" sent to {}", subject, to);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Password reset email could not be sent to {}: {}", to, e.getMessage());
        }
    }

    static String text(String link) {
        return """
                Hemos recibido una petición para cambiar la contraseña del panel de BG Sistemas.

                Crea una nueva contraseña con este enlace (válido 30 minutos y de un solo uso):
                %s

                Si no lo has pedido tú, ignora este correo: tu contraseña no cambia.
                """.formatted(link);
    }

    static String noAccountText() {
        return """
                Alguien ha pedido recuperar la contraseña del panel de BG Sistemas Informáticos con este correo,
                pero no hay ninguna cuenta de administrador asociada a él.

                Si has sido tú, prueba con el email con el que entras al panel o pide ayuda a otro administrador.
                Si no has sido tú, ignora este mensaje: no tienes que hacer nada.
                """;
    }

    static String noAccountHtml() {
        return frame("""
                <h1 style="margin:0 0 12px;font-size:19px;color:#0b1f3a;">No hay ninguna cuenta con este correo</h1>
                <p style="margin:0 0 16px;font-size:14px;line-height:1.5;">Alguien ha pedido recuperar la contraseña del panel de administración de BG Sistemas Informáticos con este correo, pero <strong>no hay ninguna cuenta de administrador</strong> asociada a él.</p>
                <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.5;">Si has sido tú, prueba con el email con el que entras al panel o pide ayuda a otro administrador.<br>Si no has sido tú, ignora este mensaje: no tienes que hacer nada.</p>
                """);
    }

    static String html(String link) {
        return frame("""
                <h1 style="margin:0 0 12px;font-size:19px;color:#0b1f3a;">Crea una nueva contraseña</h1>
                <p style="margin:0 0 16px;font-size:14px;line-height:1.5;">Hemos recibido una petición para cambiar la contraseña del panel de administración. Pulsa el botón para crear una nueva.</p>
                <p style="margin:0 0 16px;">
                  <a href="%s" style="display:inline-block;padding:10px 16px;background:#f28c28;color:#ffffff;border-radius:6px;font-weight:bold;text-decoration:none;">Crear nueva contraseña</a>
                </p>
                <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.5;">El enlace es válido <strong>30 minutos</strong> y solo se puede usar una vez.<br>Si no lo has pedido tú, ignora este correo: tu contraseña no cambia.</p>
                """.formatted(link));
    }

    // Same look for both emails: navy header with the logo, orange line and white card
    private static String frame(String content) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <body style="margin:0;padding:24px;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e5e9f0;border-radius:8px;">
                    <tr>
                      <td style="background:#0b1f3a;padding:16px 20px;">
                        <img src="cid:logo" alt="BG Sistemas Informáticos" height="34" style="display:block;height:34px;">
                      </td>
                    </tr>
                    <tr><td style="height:4px;background:#f28c28;font-size:0;line-height:0;">&nbsp;</td></tr>
                    <tr>
                      <td style="padding:20px;">
                %s
                      </td>
                    </tr>
                  </table>
                  <p style="margin:12px 0 0;text-align:center;font-size:11px;color:#9ca3af;">BG Sistemas Informáticos · Correo automático, no respondas a este mensaje</p>
                </body>
                </html>
                """.formatted(content);
    }
}
