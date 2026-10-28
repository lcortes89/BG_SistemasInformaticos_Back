package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.service.exceptions.ServiceNotFoundException;
import com.bgsistemas.api.service.mappers.ServiceMapper;
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

// BGSI-309: PATCH /api/admin/services/{id}/active
@Import({SecurityConfig.class, ServiceMapper.class})
@WebMvcTest(AdminServiceController.class)
class AdminServiceActiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private Service inactiveService() {
        Service service = new Service(1L, "BDP One", "Multiplatform version", null);
        service.setBrand(new Brand(5L, "BDP", "Software", "software", null, true));
        service.setActive(false);
        return service;
    }

    private static final String INACTIVE_JSON = "{ \"active\": false }";

    @Test
    void shouldInactivate() throws Exception {
        when(serviceService.setServiceActive(1L, false)).thenReturn(inactiveService());

        mockMvc.perform(patch("/api/admin/services/1/active").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(INACTIVE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(false));

        verify(serviceService).setServiceActive(1L, false);
    }

    @Test
    void shouldReturn400WhenTheStateIsMissing() throws Exception {
        mockMvc.perform(patch("/api/admin/services/1/active").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.active").value("Active is required"));

        verify(serviceService, never()).setServiceActive(anyLong(), anyBoolean());
    }

    @Test
    void shouldReturn404WhenItDoesNotExist() throws Exception {
        when(serviceService.setServiceActive(99L, false)).thenThrow(new ServiceNotFoundException(99L));

        mockMvc.perform(patch("/api/admin/services/99/active").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(INACTIVE_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Service with id 99 not found"));
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(patch("/api/admin/services/1/active")
                        .contentType(MediaType.APPLICATION_JSON).content(INACTIVE_JSON))
                .andExpect(status().isUnauthorized());

        verify(serviceService, never()).setServiceActive(anyLong(), anyBoolean());
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(patch("/api/admin/services/1/active").with(user("visitor@mail.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(INACTIVE_JSON))
                .andExpect(status().isForbidden());
    }
}
