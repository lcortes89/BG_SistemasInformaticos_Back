package com.bgsistemas.api.contact;
import com.bgsistemas.api.config.SecurityConfig;
import org.springframework.context.annotation.Import;

import com.bgsistemas.api.contact.dtos.ContactResponseDTO;
import com.bgsistemas.api.contact.mappers.ContactMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(ContactController.class)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private ContactMapper contactMapper;

    private static final String VALID_JSON = """
            {
              "name": "Ana López",
              "email": "ana@mail.com",
              "phone": "600123123",
              "interestedService": "BDP One",
              "message": "I would like a demo",
              "gdprConsent": true
            }
            """;

    @Test
    void shouldCreateContactAndReturn201() throws Exception {
        Contact contact = new Contact("Ana López", "ana@mail.com", "600123123",
                "BDP One", "I would like a demo", true);
        ContactResponseDTO response = new ContactResponseDTO(1L, "Ana López", "ana@mail.com", "600123123",
                "BDP One", "I would like a demo", "NEW", LocalDateTime.now(), null);

        when(contactMapper.toEntity(any())).thenReturn(contact);
        when(contactService.createContact(contact)).thenReturn(contact);
        when(contactMapper.toResponse(contact)).thenReturn(response);

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void shouldReturn400WhenGdprConsentIsNotGiven() throws Exception {
        String json = VALID_JSON.replace("\"gdprConsent\": true", "\"gdprConsent\": false");

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(contactService, never()).createContact(any());
    }

    @Test
    void shouldReturn400WhenNameIsMissing() throws Exception {
        String json = VALID_JSON.replace("\"name\": \"Ana López\"", "\"name\": \"\"");

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(contactService, never()).createContact(any());
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        String json = VALID_JSON.replace("ana@mail.com", "not-an-email");

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(contactService, never()).createContact(any());
    }
}
