package com.bgsistemas.api.brand;

import com.bgsistemas.api.brand.dtos.BrandDTO;
import com.bgsistemas.api.brand.mappers.BrandMapper;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceService;
import com.bgsistemas.api.service.dtos.ServiceDTO;
import com.bgsistemas.api.service.mappers.ServiceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(BrandController.class)
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private BrandMapper brandMapper;

    @MockitoBean
    private ServiceService serviceService;

    @MockitoBean
    private ServiceMapper serviceMapper;

    @Test
    void shouldReturnActiveBrands() throws Exception {
        Brand brand = new Brand(1L, "BDP", "Software para hostelería", "software", "bdp-logo.png", true);
        BrandDTO dto = new BrandDTO(1L, "BDP", "Software para hostelería", "software", "bdp-logo.png");

        when(brandService.getActiveBrands()).thenReturn(List.of(brand));
        when(brandMapper.toDTO(brand)).thenReturn(dto);

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("BDP"));
    }

    // BGSI-246: the products of a brand come one page at a time
    @Test
    void shouldReturnOnePageOfServicesOfABrand() throws Exception {
        Service bdpOne = new Service(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        ServiceDTO dto = new ServiceDTO(2L, "BDP One", "Multiplatform version", "bdp-one.jpg", 1L, "BDP");
        Pageable firstPage = PageRequest.of(0, 4);

        when(serviceService.getVisibleServicesPage(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(bdpOne), firstPage, 6));
        when(serviceMapper.toDTO(bdpOne)).thenReturn(dto);

        mockMvc.perform(get("/api/brands/1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("BDP One"))
                .andExpect(jsonPath("$.content[0].brandName").value("BDP"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        // 4 products per page by default
        verify(serviceService).getVisibleServicesPage(eq(1L), argThat(pageable -> pageable.getPageSize() == 4));
    }

    @Test
    void shouldNeverAskForMoreThan50ServicesAtOnce() throws Exception {
        when(serviceService.getVisibleServicesPage(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        mockMvc.perform(get("/api/brands/1/services").param("page", "0").param("size", "1000"))
                .andExpect(status().isOk());

        verify(serviceService).getVisibleServicesPage(eq(1L), argThat(pageable -> pageable.getPageSize() == 50));
    }
}
