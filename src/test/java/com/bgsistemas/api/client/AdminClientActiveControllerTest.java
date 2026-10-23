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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-309: PATCH /api/admin/clients/{id}/active
@Import({SecurityConfig.class, ClientMapper.class})
@WebMvcTest(AdminClientController.class)
class AdminClientActiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private static final String INACTIVE_JSON = "{ \"active\": false }";

    @Test
    void shouldInactivate() throws Exception {
        when(clientService.setClientPublished(1L, false)).thenReturn(new Client(1L, "Bar La Plaza", "Bar", "Great service", null, 5, false));

        mockMvc.perform(patch("/api/admin/clients/1/active").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(INACTIVE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.published").value(false));

        verify(clientService).setClientPublished(1L, false);
    }

    @Test
    void shouldReturn400WhenTheStateIsMissing() throws Exception {
        mockMvc.perform(patch("/api/admin/clients/1/active").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.active").value("Active is required"));

        verify(clientService, never()).setClientPublished(anyLong(), anyBoolean());
    }

    @Test
    void shouldReturn404WhenItDoesNotExist() throws Exception {
        when(clientService.setClientPublished(99L, false)).thenThrow(new ClientNotFoundException(99L));

        mockMvc.perform(patch("/api/admin/clients/99/active").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(INACTIVE_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Client with id 99 not found"));
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(patch("/api/admin/clients/1/active")
                        .contentType(MediaType.APPLICATION_JSON).content(INACTIVE_JSON))
                .andExpect(status().isUnauthorized());

        verify(clientService, never()).setClientPublished(anyLong(), anyBoolean());
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(patch("/api/admin/clients/1/active").with(user("visitor@mail.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(INACTIVE_JSON))
                .andExpect(status().isForbidden());
    }
}
