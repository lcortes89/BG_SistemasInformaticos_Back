package com.bgsistemas.api.brand;

import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-246: whole application + H2. A brand with 6 products is shown in pages of 4
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BrandServicesPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private Long brandId;

    @BeforeEach
    void createABrandWithSixProducts() {
        Brand brand = brandRepository.save(new Brand(null, "AHORA One", "ERP", "software", null, true));
        brandId = brand.getId();
        for (int i = 1; i <= 6; i++) {
            Service service = new Service(null, "Module " + i, "Description " + i, null);
            service.setBrand(brand);
            serviceRepository.save(service);
        }
    }

    @Test
    void firstPageHasFourProductsAndSecondPageHasTwo() throws Exception {
        mockMvc.perform(get("/api/brands/" + brandId + "/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(4))
                .andExpect(jsonPath("$.content[0].name").value("Module 1"))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/brands/" + brandId + "/services").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Module 5"))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void returns404WhenTheBrandDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/brands/999999/services"))
                .andExpect(status().isNotFound());
    }
}
