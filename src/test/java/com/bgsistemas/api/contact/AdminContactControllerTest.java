package com.bgsistemas.api.contact;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.contact.dtos.ContactResponseDTO;
import com.bgsistemas.api.contact.exceptions.ContactNotFoundException;
import com.bgsistemas.api.contact.mappers.ContactMapper;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(AdminContactController.class)
class AdminContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private ContactMapper contactMapper;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private final Contact contact = new Contact("Ana López", "ana@mail.com", "600123123", "BDP One", "Demo please", true);

    private ContactResponseDTO responseWith(String status, String note) {
        return new ContactResponseDTO(1L, "Ana López", "ana@mail.com", "600123123",
                "BDP One", "Demo please", status, LocalDateTime.now(), note);
    }

    // ---------- GET ----------

    @Test
    void shouldReturnContactsPageToAdmin() throws Exception {
        when(contactService.getContacts(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contact), PageRequest.of(0, 10), 1));
        when(contactMapper.toResponse(contact)).thenReturn(responseWith("NEW", null));

        mockMvc.perform(get("/api/admin/contacts").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ana López"))
                .andExpect(jsonPath("$.content[0].status").value("NEW"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldFilterByStatusAndSortNewestFirst() throws Exception {
        when(contactService.getContacts(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/admin/contacts").with(user(admin))
                        .param("status", "CONTACTED").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(contactService).getContacts(eq(ContactStatus.CONTACTED), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldReturn400WhenStatusDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/admin/contacts").with(user(admin)).param("status", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(get("/api/admin/contacts"))
                .andExpect(status().isUnauthorized());

        verify(contactService, never()).getContacts(any(), any());
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/contacts").with(user("visitor@mail.com").roles("USER")))
                .andExpect(status().isForbidden());

        verify(contactService, never()).getContacts(any(), any());
    }

    // ---------- PATCH ----------

    @Test
    void shouldUpdateStatusAndNote() throws Exception {
        when(contactService.updateContact(1L, ContactStatus.CONTACTED, "Called on Monday")).thenReturn(contact);
        when(contactMapper.toResponse(contact)).thenReturn(responseWith("CONTACTED", "Called on Monday"));

        mockMvc.perform(patch("/api/admin/contacts/1").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"CONTACTED\", \"note\": \"Called on Monday\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"))
                .andExpect(jsonPath("$.note").value("Called on Monday"));
    }

    @Test
    void shouldArchiveContact() throws Exception {
        when(contactService.updateContact(1L, ContactStatus.ARCHIVED, null)).thenReturn(contact);
        when(contactMapper.toResponse(contact)).thenReturn(responseWith("ARCHIVED", null));

        mockMvc.perform(patch("/api/admin/contacts/1").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"ARCHIVED\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void shouldReturn404WhenContactDoesNotExist() throws Exception {
        when(contactService.updateContact(eq(99L), any(), any())).thenThrow(new ContactNotFoundException(99L));

        mockMvc.perform(patch("/api/admin/contacts/99").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"ARCHIVED\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Contact with id 99 not found"));
    }

    @Test
    void shouldReturn400WhenStatusInBodyDoesNotExist() throws Exception {
        mockMvc.perform(patch("/api/admin/contacts/1").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"FOO\" }"))
                .andExpect(status().isBadRequest());

        verify(contactService, never()).updateContact(any(), any(), any());
    }

    @Test
    void shouldReturn400WhenNoteIsTooLong() throws Exception {
        String longNote = "a".repeat(1001);

        mockMvc.perform(patch("/api/admin/contacts/1").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"note\": \"" + longNote + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.note").exists());

        verify(contactService, never()).updateContact(any(), any(), any());
    }

    @Test
    void shouldReturn401OnUpdateWithoutSession() throws Exception {
        mockMvc.perform(patch("/api/admin/contacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"ARCHIVED\" }"))
                .andExpect(status().isUnauthorized());

        verify(contactService, never()).updateContact(any(), any(), any());
    }
}
