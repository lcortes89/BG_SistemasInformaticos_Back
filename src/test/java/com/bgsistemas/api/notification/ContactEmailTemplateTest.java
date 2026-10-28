package com.bgsistemas.api.notification;

import com.bgsistemas.api.contact.Contact;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactEmailTemplateTest {

    private final ContactEmailTemplate template = new ContactEmailTemplate("http://localhost:5173/admin/contactos");

    private Contact contact(String phone, String service, String message) {
        return new Contact("Ana López", "ana@mail.com", phone, service, message, true);
    }

    @Test
    void subjectHasTheNameAndTheService() {
        assertThat(template.subject(contact("600123123", "BDP One", "Hola")))
                .isEqualTo("Nuevo contacto: Ana López · BDP One");
    }

    @Test
    void subjectHasOnlyTheNameWhenThereIsNoService() {
        assertThat(template.subject(contact("600123123", null, "Hola")))
                .isEqualTo("Nuevo contacto: Ana López");
    }

    @Test
    void htmlShowsTheDataTheLogoAndTheButtons() {
        String html = template.html(contact("600123123", "BDP One", "Quiero una demo\npara la terraza"));

        assertThat(html)
                .contains("Ana López", "ana@mail.com", "600123123", "BDP One")
                .contains("Quiero una demo<br>para la terraza")
                .contains("cid:logo")
                .contains("href=\"http://localhost:5173/admin/contactos\"")
                .contains("href=\"mailto:ana@mail.com\"", "Responder a Ana");
    }

    @Test
    void htmlShowsADashForTheEmptyFields() {
        String html = template.html(contact(null, "", "Hola"));

        assertThat(html).contains(">—</td>");
    }

    // BGSI-121: whatever the visitor writes cannot add HTML or scripts to the email
    @Test
    void htmlEscapesWhatTheVisitorWrites() {
        String html = template.html(contact("600123123", "BDP One", "<script>alert('x')</script>"));

        assertThat(html)
                .doesNotContain("<script>")
                .contains("&lt;script&gt;");
    }

    @Test
    void plainTextHasTheSameData() {
        String text = template.text(contact("600123123", "BDP One", "Quiero una demo"));

        assertThat(text)
                .contains("Nombre:    Ana López", "Email:     ana@mail.com", "Servicio:  BDP One")
                .contains("Quiero una demo", "http://localhost:5173/admin/contactos");
    }
}
