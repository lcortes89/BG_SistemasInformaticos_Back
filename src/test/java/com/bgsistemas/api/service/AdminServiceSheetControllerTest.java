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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-205: the admin saves the full product sheet through the API
@Import({SecurityConfig.class, ServiceMapper.class})
@WebMvcTest(AdminServiceController.class)
class AdminServiceSheetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    @Test
    void shouldCreateAServiceWithItsSheet() throws Exception {
        Service saved = new Service(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        saved.setLongDescription("TPV for Windows and Android.");
        saved.setFeatures(List.of("Orders at the table"));
        saved.setSectors(List.of("Bars"));
        when(serviceService.createService(any(Service.class), eq(5L))).thenReturn(saved);

        mockMvc.perform(post("/api/admin/services").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "BDP One", "brandId": 5, "featured": false,
                                  "longDescription": "TPV for Windows and Android.",
                                  "features": ["Orders at the table"], "sectors": ["Bars"] }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.longDescription").value("TPV for Windows and Android."))
                .andExpect(jsonPath("$.features[0]").value("Orders at the table"))
                .andExpect(jsonPath("$.sectors[0]").value("Bars"));
    }

    @Test
    void shouldReturn400WithMoreThanSixFeatures() throws Exception {
        mockMvc.perform(post("/api/admin/services").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "BDP One", "brandId": 5, "featured": false,
                                  "features": ["1", "2", "3", "4", "5", "6", "7"] }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.features").value("At most 6 features"));

        verify(serviceService, never()).createService(any(), any());
    }
}
