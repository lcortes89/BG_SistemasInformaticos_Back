package com.bgsistemas.api.brand;

import com.bgsistemas.api.brand.exceptions.BrandInUseException;
import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import com.bgsistemas.api.service.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private BrandServiceImpl brandService;

    private Brand bdp() {
        return new Brand(1L, "BDP", "Software para hostelería", "software", "bdp-logo.png", true);
    }

    @Test
    void shouldReturnOnlyActiveBrands() {
        when(brandRepository.findByActiveTrue()).thenReturn(List.of(bdp()));

        List<Brand> result = brandService.getActiveBrands();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("BDP");
    }

    @Test
    void shouldReturnBrandById() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(bdp()));

        Brand result = brandService.getBrandById(1L);

        assertThat(result.getName()).isEqualTo("BDP");
    }

    @Test
    void shouldThrowWhenBrandDoesNotExist() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.getBrandById(99L))
                .isInstanceOf(BrandNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldCreateBrandAsNewRow() {
        Brand newBrand = new Brand(50L, "Epson", null, "hardware", null, true);
        when(brandRepository.save(newBrand)).thenReturn(newBrand);

        brandService.createBrand(newBrand);

        assertThat(newBrand.getId()).isNull();
        verify(brandRepository).save(newBrand);
    }

    @Test
    void shouldUpdateAllFieldsOfTheBrand() {
        Brand existing = bdp();
        Brand changes = new Brand(null, "BDP Software", "New text", "software", null, false);
        when(brandRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepository.save(existing)).thenReturn(existing);

        Brand result = brandService.updateBrand(1L, changes);

        assertThat(result.getName()).isEqualTo("BDP Software");
        assertThat(result.getDescription()).isEqualTo("New text");
        assertThat(result.getLogoUrl()).isNull();
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void shouldDeleteBrandWithoutServices() {
        Brand brand = bdp();
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(serviceRepository.countByBrandId(1L)).thenReturn(0L);

        brandService.deleteBrand(1L);

        verify(brandRepository).delete(brand);
    }

    @Test
    void shouldNotDeleteBrandWithServices() {
        when(brandRepository.findById(1L)).thenReturn(Optional.of(bdp()));
        when(serviceRepository.countByBrandId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> brandService.deleteBrand(1L))
                .isInstanceOf(BrandInUseException.class)
                .hasMessageContaining("3 service(s)");

        verify(brandRepository, never()).delete(any());
    }

    @Test
    void shouldThrowWhenDeletingUnknownBrand() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.deleteBrand(99L))
                .isInstanceOf(BrandNotFoundException.class);
    }
}
