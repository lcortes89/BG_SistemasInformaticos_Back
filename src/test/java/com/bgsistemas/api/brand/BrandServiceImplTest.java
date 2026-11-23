package com.bgsistemas.api.brand;

import com.bgsistemas.api.brand.exceptions.BrandInUseException;
import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import com.bgsistemas.api.service.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bgsistemas.api.globals.BulkResult;

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

    // BGSI-309
    @Test
    void shouldInactivateBrand() {
        Brand brand = bdp();
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.save(brand)).thenReturn(brand);

        Brand result = brandService.setBrandActive(1L, false);

        assertThat(result.isActive()).isFalse();
        verify(brandRepository).save(brand);
    }

    @Test
    void shouldThrowWhenChangingTheStateOfAnUnknownBrand() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.setBrandActive(99L, true))
                .isInstanceOf(BrandNotFoundException.class);

        verify(brandRepository, never()).save(any());
    }

    // ---------- BGSI-333: bulk actions ----------

    private Brand brand(long id, String name) {
        return new Brand(id, name, "Description", "software", null, true);
    }

    @Test
    void shouldDeleteTheBrandsWithoutProductsAndReportTheOthers() {
        Brand empty = brand(1L, "Empty");
        Brand withProducts = brand(2L, "BDP");
        when(brandRepository.findAllById(List.of(1L, 2L, 99L))).thenReturn(List.of(empty, withProducts));
        when(serviceRepository.countByBrandId(1L)).thenReturn(0L);
        when(serviceRepository.countByBrandId(2L)).thenReturn(3L);

        BulkResult result = brandService.deleteBrands(List.of(1L, 2L, 99L));

        verify(brandRepository).deleteAll(List.of(empty));
        assertThat(result.processed()).containsExactly(1L);
        assertThat(result.failed()).containsExactlyInAnyOrder(
                new BulkResult.Failure(2L, BulkResult.HAS_PRODUCTS),
                new BulkResult.Failure(99L, BulkResult.NOT_FOUND));
    }

    @Test
    void shouldHideOrShowSeveralBrands() {
        Brand first = brand(1L, "BDP");
        Brand second = brand(2L, "PUDU");
        when(brandRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        BulkResult result = brandService.setBrandsActive(List.of(1L, 2L), false);

        assertThat(first.isActive()).isFalse();
        assertThat(second.isActive()).isFalse();
        verify(brandRepository).saveAll(List.of(first, second));
        assertThat(result.processed()).containsExactly(1L, 2L);
    }
}
