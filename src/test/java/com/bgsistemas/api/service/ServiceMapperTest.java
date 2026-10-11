package com.bgsistemas.api.service;

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
}