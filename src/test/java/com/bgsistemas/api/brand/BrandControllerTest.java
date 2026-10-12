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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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

    @Test
    void shouldReturnServicesOfABrand() throws Exception {
        Service bdpOne = new Service(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        ServiceDTO dto = new ServiceDTO(2L, "BDP One", "Multiplatform version", "bdp-one.jpg", 1L, "BDP");

        when(serviceService.getServicesByBrandId(1L)).thenReturn(List.of(bdpOne));
        when(serviceMapper.toDTO(bdpOne)).thenReturn(dto);

        mockMvc.perform(get("/api/brands/1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("BDP One"))
                .andExpect(jsonPath("$[0].brandName").value("BDP"));
    }
}
