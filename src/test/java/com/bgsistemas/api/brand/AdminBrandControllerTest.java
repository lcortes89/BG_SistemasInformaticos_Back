package com.bgsistemas.api.brand;

import com.bgsistemas.api.brand.exceptions.BrandInUseException;
import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import com.bgsistemas.api.brand.mappers.BrandMapper;
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
import static org.mockito.Mockito.doThrow;
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

// The real BrandMapper is used, so the JSON is checked end to end
@Import({SecurityConfig.class, BrandMapper.class})
@WebMvcTest(AdminBrandController.class)
class AdminBrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandService brandService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private final Brand bdp = new Brand(1L, "BDP", "Software para hostelería", "software", "bdp.png", true);

    private static final String VALID_JSON = """
            { "name": "BDP", "description": "Software para hostelería", "category": "software",
              "logoUrl": "bdp.png", "active": true }
            """;

    @Test
    void shouldReturnBrandsPageWithServiceCount() throws Exception {
        when(brandService.getBrandsPage(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bdp), PageRequest.of(0, 10), 1));
        when(brandService.countServices(1L)).thenReturn(2L);

        mockMvc.perform(get("/api/admin/brands").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("BDP"))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].serviceCount").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // BGSI-302: search box + active
    @Test
    void shouldPassTheSearchAndTheActiveFilter() throws Exception {
        when(brandService.getBrandsPage(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/brands").with(user(admin)).param("q", "bdp").param("active", "false"))
                .andExpect(status().isOk());

        verify(brandService).getBrandsPage(eq("bdp"), eq(false), any(Pageable.class));
    }

    @Test
    void shouldCreateBrand() throws Exception {
        when(brandService.createBrand(any(Brand.class))).thenReturn(bdp);

        mockMvc.perform(post("/api/admin/brands").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("BDP"));
    }

    @Test
    void shouldReturn400WhenNameAndCategoryAreMissing() throws Exception {
        mockMvc.perform(post("/api/admin/brands").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"\", \"active\": true }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("Name is required"))
                .andExpect(jsonPath("$.details.category").value("Category is required"));

        verify(brandService, never()).createBrand(any());
    }

    @Test
    void shouldUpdateBrand() throws Exception {
        when(brandService.updateBrand(eq(1L), any(Brand.class))).thenReturn(bdp);

        mockMvc.perform(put("/api/admin/brands/1").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BDP"));
    }

    @Test
    void shouldReturn404WhenUpdatingUnknownBrand() throws Exception {
        when(brandService.updateBrand(eq(99L), any(Brand.class))).thenThrow(new BrandNotFoundException(99L));

        mockMvc.perform(put("/api/admin/brands/99").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteBrand() throws Exception {
        mockMvc.perform(delete("/api/admin/brands/1").with(user(admin)))
                .andExpect(status().isNoContent());

        verify(brandService).deleteBrand(1L);
    }

    @Test
    void shouldReturn409WhenBrandHasServices() throws Exception {
        doThrow(new BrandInUseException("BDP", 3)).when(brandService).deleteBrand(1L);

        mockMvc.perform(delete("/api/admin/brands/1").with(user(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(delete("/api/admin/brands/1"))
                .andExpect(status().isUnauthorized());

        verify(brandService, never()).deleteBrand(any());
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/brands").with(user("visitor@mail.com").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
