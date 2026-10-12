package com.bgsistemas.api.client;

import com.bgsistemas.api.client.exceptions.ClientNotFoundException;
import com.bgsistemas.api.client.mappers.ClientMapper;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The real ClientMapper is used, so the JSON is checked end to end
@Import({SecurityConfig.class, ClientMapper.class})
@WebMvcTest(AdminClientController.class)
class AdminClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private final Client laPlaza = new Client(1L, "Bar La Plaza", "Bar", "Great service", "laplaza.png", 5, false);

    private static final String VALID_JSON = """
            { "name": "Bar La Plaza", "businessType": "Bar", "testimonial": "Great service",
              "logoUrl": "laplaza.png", "rating": 5, "published": false }
            """;

    @Test
    void shouldReturnClientsPageIncludingUnpublished() throws Exception {
        when(clientService.getClientsPage(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(laPlaza), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/clients").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bar La Plaza"))
                .andExpect(jsonPath("$.content[0].published").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldCreateClient() throws Exception {
        when(clientService.createClient(any(Client.class))).thenReturn(laPlaza);

        mockMvc.perform(post("/api/admin/clients").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/admin/clients").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"\", \"published\": true }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("Name is required"))
                .andExpect(jsonPath("$.details.businessType").value("Business type is required"))
                .andExpect(jsonPath("$.details.testimonial").value("Testimonial is required"))
                .andExpect(jsonPath("$.details.rating").value("Rating is required"));

        verify(clientService, never()).createClient(any());
    }

    @Test
    void shouldReturn400WhenRatingIsOutOfRange() throws Exception {
        mockMvc.perform(post("/api/admin/clients").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON.replace("\"rating\": 5", "\"rating\": 6")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.rating").value("Rating must be between 1 and 5"));
    }

    @Test
    void shouldUpdateClient() throws Exception {
        when(clientService.updateClient(eq(1L), any(Client.class))).thenReturn(laPlaza);

        mockMvc.perform(put("/api/admin/clients/1").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bar La Plaza"));
    }

    @Test
    void shouldReturn404WhenClientDoesNotExist() throws Exception {
        when(clientService.updateClient(eq(99L), any(Client.class))).thenThrow(new ClientNotFoundException(99L));

        mockMvc.perform(put("/api/admin/clients/99").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client with id 99 not found"));
    }

    @Test
    void shouldDeleteClient() throws Exception {
        mockMvc.perform(delete("/api/admin/clients/1").with(user(admin)))
                .andExpect(status().isNoContent());

        verify(clientService).deleteClient(1L);
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(delete("/api/admin/clients/1"))
                .andExpect(status().isUnauthorized());

        verify(clientService, never()).deleteClient(any());
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/clients").with(user("visitor@mail.com").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
