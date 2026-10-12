package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceMapperTest {

    @Test
    void shouldMapServiceToServiceDTO() {
        Service service = new Service(1L, "Web Design", "Custom website design", "design.jpg");

        ServiceMapper mapper = new ServiceMapper();
        ServiceDTO dto = mapper.toDTO(service);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Web Design");
        assertThat(dto.getDescription()).isEqualTo("Custom website design");
        assertThat(dto.getImageUrl()).isEqualTo("design.jpg");
    }

    // NEW: when the service has a brand, the DTO includes its id and name
    @Test
    void shouldIncludeBrandDataInDTO() {
        Brand bdp = new Brand(5L, "BDP", "Hospitality software", "Software", "bdp.png", true);
        Service service = new Service(1L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        service.setBrand(bdp);

        ServiceDTO dto = new ServiceMapper().toDTO(service);

        assertThat(dto.getBrandId()).isEqualTo(5L);
        assertThat(dto.getBrandName()).isEqualTo("BDP");
    }

    // NEW: a service without a brand must not break the mapper (no NullPointerException)
    @Test
    void shouldMapServiceWithoutBrand() {
        Service service = new Service(2L, "Support", "Technical support", "support.jpg");

        ServiceDTO dto = new ServiceMapper().toDTO(service);

        assertThat(dto.getBrandId()).isNull();
        assertThat(dto.getBrandName()).isNull();
    }
}