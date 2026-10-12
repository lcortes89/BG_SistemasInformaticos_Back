package com.bgsistemas.api.brand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @Test
    void shouldReturnAllBrands() {
        Brand brand = new Brand(1L, "BDP", "Software para hostelería", "software", "bdp-logo.png", true);
        when(brandRepository.findAll()).thenReturn(List.of(brand));

        BrandServiceImpl brandService = new BrandServiceImpl(brandRepository);
        List<Brand> result = brandService.getAllBrands();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("BDP");
    }
}