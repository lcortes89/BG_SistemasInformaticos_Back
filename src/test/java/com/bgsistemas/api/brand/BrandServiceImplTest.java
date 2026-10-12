package com.bgsistemas.api.brand;

import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void shouldReturnBrandById() {
        Brand brand = new Brand(1L, "BDP", "Software para hostelería", "software", "bdp-logo.png", true);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        Brand result = new BrandServiceImpl(brandRepository).getBrandById(1L);

        assertThat(result.getName()).isEqualTo("BDP");
    }

    @Test
    void shouldThrowWhenBrandDoesNotExist() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new BrandServiceImpl(brandRepository).getBrandById(99L))
                .isInstanceOf(BrandNotFoundException.class)
                .hasMessageContaining("99");
    }
}
