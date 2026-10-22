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
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

// BGSI-273: the "Crea una nueva contraseña" email, sent with the same Gmail account as the contact emails.
// BGSI-291: also the security alert for the admins when many requests use unknown emails
@Component
public class PasswordResetEmailSender {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailSender.class);
    private static final ClassPathResource LOGO = new ClassPathResource("pdf/logo.png");
    static final String SUBJECT = "Crea una nueva contraseña · Panel de BG Sistemas";
    static final String ALERT_SUBJECT = "Aviso de seguridad: intentos de recuperar contraseña · Panel de BG Sistemas";
    static final String INVITATION_SUBJECT = "Te han invitado al panel de BG Sistemas";
    static final String NOTICE_SUBJECT = "Cambio en los administradores · Panel de BG Sistemas";
    static final String EMAIL_CHANGE_SUBJECT = "Confirma tu nuevo email · Panel de BG Sistemas";
    static final String EMAIL_CHANGE_REQUESTED_SUBJECT = "Aviso de seguridad: cambio de email · Panel de BG Sistemas";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailSettingsService mailSettingsService;
    private final String resetUrl;

    // BGSI-299: page of the front that opens the "confirm your new email" link
    @Value("${app.email-change.url:http://localhost:5173/admin/confirmar-email}")
    private String emailChangeUrl = "http://localhost:5173/admin/confirmar-email";

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
        deliver(new String[] {to}, SUBJECT, text(link(token)), html(link(token)));
    }

    // BGSI-291: many requests with emails that are not an admin -> one email to every admin
    @Async
    public void sendUnknownEmailAlert(List<String> admins, UnknownEmailAlarm.Alert alert) {
        deliver(admins.toArray(String[]::new), ALERT_SUBJECT, alertText(alert), alertHtml(alert));
    }

    // BGSI-277: the same page as "create a new password", with a welcome text (invitacion=1)
    String invitationLink(String token) {
        return link(token) + "&invitacion=1";
    }

    @Async
    public void sendInvitation(String to, String token, String invitedBy) {
        String link = invitationLink(token);
        deliver(new String[] {to}, INVITATION_SUBJECT, invitationText(link, invitedBy), invitationHtml(link, invitedBy));
    }

    // BGSI-277: "X has blocked Y" to every active admin, so a stolen session cannot act in silence
    @Async
    public void sendAdminNotice(List<String> to, String message) {
        if (to.isEmpty()) {
            return;
        }
        deliver(to.toArray(String[]::new), NOTICE_SUBJECT, noticeText(message), noticeHtml(message));
    }

    // BGSI-299: the link goes to the NEW address, so it is proved that the admin owns it
    String emailChangeLink(String token) {
        return emailChangeUrl + "?token=" + token;
    }

    @Async
    public void sendEmailChangeLink(String to, String token, String oldEmail) {
        String link = emailChangeLink(token);
        deliver(new String[] {to}, EMAIL_CHANGE_SUBJECT, emailChangeText(link, oldEmail), emailChangeHtml(link, oldEmail));
    }

    // BGSI-299: notice to the OLD address, in case somebody else is using the session
    @Async
    public void sendEmailChangeRequested(String oldEmail, String newEmail) {
        deliver(new String[] {oldEmail}, EMAIL_CHANGE_REQUESTED_SUBJECT,
                emailChangeRequestedText(newEmail), emailChangeRequestedHtml(newEmail));
    }

    private void deliver(String[] to, String subject, String text, String html) {
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
            log.info("Password reset email \"{}\" sent to {}", subject, String.join(", ", to));
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Password reset email could not be sent to {}: {}", String.join(", ", to), e.getMessage());
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

    static String alertText(UnknownEmailAlarm.Alert alert) {
        return """
                En los últimos 15 minutos se ha pedido %d veces recuperar la contraseña del panel de BG Sistemas
                con emails que no son de ningún administrador:
                %s

                Puede ser alguien que se ha equivocado de correo o un intento de ataque. Nadie ha podido entrar:
                a esos emails no se les ha enviado ningún enlace. Si se repite a menudo, avisa al responsable técnico.
                """.formatted(alert.attempts(), alert.emails().stream().map(email -> "- " + email).collect(Collectors.joining("\n")));
    }

    static String alertHtml(UnknownEmailAlarm.Alert alert) {
        // The emails were typed by a visitor: escaped so they cannot add HTML to the email
        String emails = alert.emails().stream()
                .map(email -> "<li>" + HtmlUtils.htmlEscape(email, "UTF-8") + "</li>")
                .collect(Collectors.joining());
        return frame("""
                <h1 style="margin:0 0 12px;font-size:19px;color:#0b1f3a;">Aviso de seguridad</h1>
                <p style="margin:0 0 12px;font-size:14px;line-height:1.5;">En los últimos 15 minutos se ha pedido <strong>%d veces</strong> recuperar la contraseña del panel con emails que <strong>no son de ningún administrador</strong>:</p>
                <ul style="margin:0 0 16px;padding-left:20px;font-size:14px;line-height:1.5;">%s</ul>
                <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.5;">Puede ser alguien que se ha equivocado de correo o un intento de ataque. Nadie ha podido entrar: a esos emails no se les ha enviado ningún enlace.<br>Si se repite a menudo, avisa al responsable técnico.</p>
                """.formatted(alert.attempts(), emails));
    }

    static String invitationText(String link, String invitedBy) {
        return """
                %s te ha invitado a ser administrador del panel de BG Sistemas.

                Crea tu contraseña con este enlace (válido 24 horas y de un solo uso):
                %s

                Si no esperabas esta invitación, ignora este correo.
                """.formatted(invitedBy, link);
    }

    static String invitationHtml(String link, String invitedBy) {
        return frame("""
                <h1 style="margin:0 0 12px;font-size:19px;color:#0b1f3a;">Te han invitado al panel</h1>
                <p style="margin:0 0 16px;font-size:14px;line-height:1.5;"><strong>%s</strong> te ha invitado a ser administrador del panel de BG Sistemas. Pulsa el botón para crear tu contraseña.</p>
                <p style="margin:0 0 16px;">
                  <a href="%s" style="display:inline-block;padding:10px 16px;background:#f28c28;color:#ffffff;border-radius:6px;font-weight:bold;text-decoration:none;">Crear mi contraseña</a>
                </p>
                <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.5;">El enlace es válido <strong>24 horas</strong> y solo se puede usar una vez.<br>Si no esperabas esta invitación, ignora este correo.</p>
                """.formatted(HtmlUtils.htmlEscape(invitedBy, "UTF-8"), link));
    }

    static String noticeText(String message) {
        return """
                %s

                Si no reconoces este cambio, entra en el panel, revisa la lista de administradores y cambia tu contraseña.
                """.formatted(message);
    }

    static String noticeHtml(String message) {
        return frame("""
                <h1 style="margin:0 0 12px;font-size:19px;color:#0b1f3a;">Cambio en los administradores</h1>
                <p style="margin:0 0 16px;font-size:14px;line-height:1.5;">%s</p>
                <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.5;">Si no reconoces este cambio, entra en el panel, revisa la lista de administradores y cambia tu contraseña.</p>
                """.formatted(HtmlUtils.htmlEscape(message, "UTF-8")));
    }

    static String emailChangeText(String link, String oldEmail) {
        return """
                Has pedido cambiar el email de tu cuenta del panel de BG Sistemas (ahora es %s) por esta dirección.

                Confírmalo con este enlace (válido 1 hora y de un solo uso):
                %s

                Al confirmarlo se cerrarán tus sesiones abiertas y tendrás que entrar con este email.
                Si no lo has pedido tú, ignora este correo: tu email no cambia.
                """.formatted(oldEmail, link);
    }

    static String emailChangeHtml(String link, String oldEmail) {
        return frame("""
                <h1 style="margin:0 0 12px;font-size:19px;color:#0b1f3a;">Confirma tu nuevo email</h1>
                <p style="margin:0 0 16px;font-size:14px;line-height:1.5;">Has pedido cambiar el email de tu cuenta del panel (ahora es <strong>%s</strong>) por esta dirección. Pulsa el botón para confirmarlo.</p>
                <p style="margin:0 0 16px;">
                  <a href="%s" style="display:inline-block;padding:10px 16px;background:#f28c28;color:#ffffff;border-radius:6px;font-weight:bold;text-decoration:none;">Confirmar mi nuevo email</a>
                </p>
                <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.5;">El enlace es válido <strong>1 hora</strong> y solo se puede usar una vez. Al confirmarlo se cerrarán tus sesiones abiertas.<br>Si no lo has pedido tú, ignora este correo: tu email no cambia.</p>
                """.formatted(HtmlUtils.htmlEscape(oldEmail, "UTF-8"), link));
    }

    static String emailChangeRequestedText(String newEmail) {
        return """
                Alguien ha pedido cambiar el email de tu cuenta del panel de BG Sistemas por %s.

                El cambio solo se hará si se confirma desde esa dirección.
                Si no has sido tú, entra en el panel, cancela el cambio en "Mi cuenta" y cambia tu contraseña.
                """.formatted(newEmail);
    }

    static String emailChangeRequestedHtml(String newEmail) {
        return frame("""
                <h1 style="margin:0 0 12px;font-size:19px;color:#0b1f3a;">Aviso de seguridad</h1>
                <p style="margin:0 0 16px;font-size:14px;line-height:1.5;">Alguien ha pedido cambiar el email de tu cuenta del panel por <strong>%s</strong>. El cambio solo se hará si se confirma desde esa dirección.</p>
                <p style="margin:0;font-size:13px;color:#6b7280;line-height:1.5;">Si no has sido tú, entra en el panel, cancela el cambio en "Mi cuenta" y cambia tu contraseña.</p>
                """.formatted(HtmlUtils.htmlEscape(newEmail, "UTF-8")));
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
