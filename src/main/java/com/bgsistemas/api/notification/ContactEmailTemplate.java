package com.bgsistemas.api.notification;

import com.bgsistemas.api.contact.Contact;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// BGSI-120: subject, plain text and branded HTML of the "new contact" email
@Component
public class ContactEmailTemplate {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm");
    private static final String EMPTY = "—";

    private final String panelUrl;

    public ContactEmailTemplate(@Value("${app.mail.panel-url:http://localhost:5173/admin/contactos}") String panelUrl) {
        this.panelUrl = panelUrl;
    }

    public String subject(Contact contact) {
        String service = contact.getInterestedService();
        return StringUtils.hasText(service)
                ? "Nuevo contacto: %s · %s".formatted(contact.getName(), service)
                : "Nuevo contacto: " + contact.getName();
    }

    // Shown by email apps that do not display HTML
    public String text(Contact contact) {
        return """
                Has recibido un nuevo contacto desde la web.

                Nombre:    %s
                Email:     %s
                Teléfono:  %s
                Servicio:  %s
                Fecha:     %s

                Mensaje:
                %s

                Gestiónalo en el panel: %s
                (Responde a este correo para escribir directamente al cliente)
                """.formatted(contact.getName(), contact.getEmail(), orEmpty(contact.getPhone()),
                orEmpty(contact.getInterestedService()), date(contact), contact.getMessage(), panelUrl);
    }

    // Inline styles and tables: most email apps ignore <style> blocks
    public String html(Contact contact) {
        String rows = row("Nombre", "<strong>" + escape(contact.getName()) + "</strong>")
                + row("Email", escape(contact.getEmail()))
                + row("Teléfono", escape(orEmpty(contact.getPhone())))
                + row("Servicio", escape(orEmpty(contact.getInterestedService())));
        String message = escape(contact.getMessage()).replace("\n", "<br>");

        return """
                <!DOCTYPE html>
                <html lang="es">
                <body style="margin:0;padding:24px;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e5e9f0;border-radius:8px;">
                    <tr>
                      <td style="background:#0b1f3a;padding:16px 20px;">
                        <img src="cid:logo" alt="BG Sistemas Informáticos" height="34" style="display:block;height:34px;">
                      </td>
                      <td style="background:#0b1f3a;padding:16px 20px;text-align:right;color:#cbd5e1;font-size:12px;">Aviso automático de la web</td>
                    </tr>
                    <tr><td colspan="2" style="height:4px;background:#f28c28;font-size:0;line-height:0;">&nbsp;</td></tr>
                    <tr>
                      <td colspan="2" style="padding:20px;">
                        <h1 style="margin:0 0 4px;font-size:19px;color:#0b1f3a;">Nuevo contacto desde la web</h1>
                        <p style="margin:0 0 16px;color:#6b7280;font-size:13px;">%s</p>
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="font-size:14px;">%s</table>
                        <div style="margin-top:14px;padding:10px 12px;background:#f8fafc;border-left:3px solid #f28c28;font-size:14px;line-height:1.5;">%s</div>
                        <p style="margin:18px 0 0;">
                          <a href="%s" style="display:inline-block;padding:10px 16px;background:#f28c28;color:#ffffff;border-radius:6px;font-weight:bold;text-decoration:none;">Ver en el panel</a>
                          &nbsp;
                          <a href="mailto:%s" style="display:inline-block;padding:9px 16px;border:1px solid #0b1f3a;color:#0b1f3a;border-radius:6px;font-weight:bold;text-decoration:none;">Responder a %s</a>
                        </p>
                      </td>
                    </tr>
                  </table>
                  <p style="margin:12px 0 0;text-align:center;font-size:11px;color:#9ca3af;">BG Sistemas Informáticos · Este correo se envía al recibir un formulario de contacto</p>
                </body>
                </html>
                """.formatted(date(contact), rows, message, escape(panelUrl), escape(contact.getEmail()),
                escape(firstName(contact.getName())));
    }

    private String row(String label, String value) {
        return "<tr><td style=\"padding:8px 0;border-bottom:1px solid #eef1f5;color:#6b7280;width:120px;\">" + label
                + "</td><td style=\"padding:8px 0;border-bottom:1px solid #eef1f5;\">" + value + "</td></tr>";
    }

    // The visitor writes these texts: escaping them stops HTML or scripts inside the email
    private String escape(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }

    private String orEmpty(String value) {
        return StringUtils.hasText(value) ? value : EMPTY;
    }

    private String date(Contact contact) {
        LocalDateTime createdAt = contact.getCreatedAt() != null ? contact.getCreatedAt() : LocalDateTime.now();
        return createdAt.format(DATE_FORMAT);
    }

    private String firstName(String name) {
        return name.trim().split("\\s+")[0];
    }
}
