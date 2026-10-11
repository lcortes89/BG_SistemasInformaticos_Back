package com.bgsistemas.api.brand;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BrandRepositoryTest {

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void shouldSaveAndRetrieveBrand() {
        Brand brand = new Brand(null, "BDP", "Software para hostelería", "software", "bdp-logo.png", true);

        Brand saved = brandRepository.save(brand);

        assertThat(saved.getId()).isNotNull();
        assertThat(brandRepository.findById(saved.getId())).isPresent();
    }
}