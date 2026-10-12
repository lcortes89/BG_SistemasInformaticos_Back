package com.bgsistemas.api.brand.mappers;
import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.dtos.BrandDTO;
import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.dtos.BrandDTO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrandMapperTest {

    @Test
    void shouldMapBrandToBrandDTO() {
        Brand brand = new Brand(1L, "BDP", "Software para hostelería", "software", "bdp-logo.png", true);

        BrandMapper mapper = new BrandMapper();
        BrandDTO dto = mapper.toDTO(brand);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("BDP");
        assertThat(dto.getDescription()).isEqualTo("Software para hostelería");
        assertThat(dto.getCategory()).isEqualTo("software");
        assertThat(dto.getLogoUrl()).isEqualTo("bdp-logo.png");
    }
}