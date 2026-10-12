package com.bgsistemas.api.brand;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrandController.class)
class BrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private BrandMapper brandMapper;

    @Test
    void shouldReturnAllBrands() throws Exception {
        Brand brand = new Brand(1L, "BDP", "Software para hostelería", "software", "bdp-logo.png", true);
        BrandDTO dto = new BrandDTO(1L, "BDP", "Software para hostelería", "software", "bdp-logo.png");

        when(brandService.getAllBrands()).thenReturn(List.of(brand));
        when(brandMapper.toDTO(brand)).thenReturn(dto);

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("BDP"));
    }
}