package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.service.mappers.ServiceMapper;
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

// The real ServiceMapper is used, so the JSON is checked end to end
@Import({SecurityConfig.class, ServiceMapper.class})
@WebMvcTest(AdminServiceController.class)
class AdminServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceService serviceService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private static final String VALID_JSON = """
            { "name": "BDP One", "description": "Multiplatform version", "imageUrl": "bdp-one.jpg",
              "featured": true, "brandId": 5 }
            """;

    private Service bdpOne() {
        Service service = new Service(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        service.setFeatured(true);
        service.setBrand(new Brand(5L, "BDP", "Software", "software", null, true));
        return service;
    }

    @Test
    void shouldReturnServicesPageWithBrand() throws Exception {
        when(serviceService.getServicesPage(eq(null), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bdpOne()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/services").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("BDP One"))
                .andExpect(jsonPath("$.content[0].featured").value(true))
                .andExpect(jsonPath("$.content[0].brandName").value("BDP"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldPassTheBrandFilter() throws Exception {
        when(serviceService.getServicesPage(eq(5L), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/admin/services").with(user(admin)).param("brandId", "5"))
                .andExpect(status().isOk());

        verify(serviceService).getServicesPage(eq(5L), eq(null), eq(null), any(Pageable.class));
    }

    // BGSI-302: search box + active, together with the brand
    @Test
    void shouldPassTheSearchAndTheActiveFilter() throws Exception {
        when(serviceService.getServicesPage(any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/services").with(user(admin))
                        .param("brandId", "5").param("q", "one").param("active", "true"))
                .andExpect(status().isOk());

        verify(serviceService).getServicesPage(eq(5L), eq("one"), eq(true), any(Pageable.class));
    }

    @Test
    void shouldCreateService() throws Exception {
        when(serviceService.createService(any(Service.class), eq(5L))).thenReturn(bdpOne());

        mockMvc.perform(post("/api/admin/services").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.brandId").value(5));
    }

    @Test
    void shouldReturn400WhenNameAndBrandAreMissing() throws Exception {
        mockMvc.perform(post("/api/admin/services").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"\", \"featured\": false }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("Name is required"))
                .andExpect(jsonPath("$.details.brandId").value("Brand is required"));

        verify(serviceService, never()).createService(any(), any());
    }

    @Test
    void shouldReturn404WhenBrandDoesNotExist() throws Exception {
        when(serviceService.createService(any(Service.class), eq(5L))).thenThrow(new BrandNotFoundException(5L));

        mockMvc.perform(post("/api/admin/services").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Brand with id 5 not found"));
    }

    @Test
    void shouldUpdateService() throws Exception {
        when(serviceService.updateService(eq(2L), any(Service.class), eq(5L))).thenReturn(bdpOne());

        mockMvc.perform(put("/api/admin/services/2").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BDP One"));
    }

    @Test
    void shouldDeleteService() throws Exception {
        mockMvc.perform(delete("/api/admin/services/2").with(user(admin)))
                .andExpect(status().isNoContent());

        verify(serviceService).deleteService(2L);
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(post("/api/admin/services")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isUnauthorized());

        verify(serviceService, never()).createService(any(), any());
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/services").with(user("visitor@mail.com").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
