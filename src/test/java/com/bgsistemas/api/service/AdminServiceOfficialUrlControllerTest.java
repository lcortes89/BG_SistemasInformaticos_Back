package com.bgsistemas.api.service;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.SecurityUser;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-205: link to the official product page, saved from the admin panel
@Import({SecurityConfig.class, ServiceMapper.class})
@WebMvcTest(AdminServiceController.class)
class AdminServiceOfficialUrlControllerTest {

    private static final String OFFICIAL_URL = "https://www.bdp-software.com/bdp-one";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    @Test
    void shouldSaveTheOfficialUrl() throws Exception {
        Service saved = new Service(2L, "BDP One", null, null);
        saved.setOfficialUrl(OFFICIAL_URL);
        when(serviceService.createService(any(Service.class), eq(5L))).thenReturn(saved);

        mockMvc.perform(post("/api/admin/services").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "BDP One", "brandId": 5, "featured": false,
                                  "officialUrl": "%s" }
                                """.formatted(OFFICIAL_URL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.officialUrl").value(OFFICIAL_URL));

        verify(serviceService).createService(argThat(s -> OFFICIAL_URL.equals(s.getOfficialUrl())), eq(5L));
    }

    @Test
    void shouldAcceptAnEmptyOfficialUrl() throws Exception {
        when(serviceService.createService(any(Service.class), eq(5L)))
                .thenReturn(new Service(2L, "BDP One", null, null));

        mockMvc.perform(post("/api/admin/services").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "BDP One", "brandId": 5, "featured": false, "officialUrl": "" }
                                """))
                .andExpect(status().isCreated());

        verify(serviceService).createService(argThat(s -> s.getOfficialUrl() == null), eq(5L));
    }

    @Test
    void shouldReturn400WhenTheOfficialUrlIsNotAWebAddress() throws Exception {
        mockMvc.perform(post("/api/admin/services").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "BDP One", "brandId": 5, "featured": false,
                                  "officialUrl": "www.bdp-software.com" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.officialUrl")
                        .value("Official URL must start with http:// or https://"));

        verify(serviceService, never()).createService(any(), any());
    }
}
