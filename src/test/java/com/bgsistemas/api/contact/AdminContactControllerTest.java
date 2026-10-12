package com.bgsistemas.api.contact;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.contact.dtos.ContactResponseDTO;
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

    @Test
    void shouldReturnContactsPageToAdmin() throws Exception {
        Contact contact = new Contact("Ana López", "ana@mail.com", "600123123", "BDP One", "Demo please", true);
        ContactResponseDTO dto = new ContactResponseDTO(1L, "Ana López", "ana@mail.com", "600123123",
                "BDP One", "Demo please", "NEW", LocalDateTime.now(), null);
        when(contactService.getContacts(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contact), PageRequest.of(0, 10), 1));
        when(contactMapper.toResponse(contact)).thenReturn(dto);

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
}
