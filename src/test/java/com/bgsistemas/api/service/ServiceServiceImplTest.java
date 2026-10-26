package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.brand.exceptions.BrandNotFoundException;
import com.bgsistemas.api.service.exceptions.ServiceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceServiceImplTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    private final PageRequest pageRequest = PageRequest.of(0, 10);
    private final Brand bdp = new Brand(5L, "BDP", "Software", "software", null, true);

    @Test
    void shouldReturnAllServices() {
        Service service = new Service(1L, "Web Design", "Custom website design", "design.jpg");
        when(serviceRepository.findVisible()).thenReturn(List.of(service));

        List<Service> result = serviceService.getAllServices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Web Design");
    }

    @Test
    void shouldReturnServicesByBrandId() {
        Service bdpNet = new Service(1L, "BDP .Net", "Classic version", "bdp-net.jpg");
        Service bdpOne = new Service(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        when(serviceRepository.findByBrandId(5L)).thenReturn(List.of(bdpNet, bdpOne));

        List<Service> result = serviceService.getServicesByBrandId(5L);

        assertThat(result).extracting(Service::getName).containsExactly("BDP .Net", "BDP One");
    }

    @Test
    void shouldReturnFeaturedServices() {
        Service bdpOne = new Service(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        bdpOne.setFeatured(true);
        when(serviceRepository.findVisibleFeatured()).thenReturn(List.of(bdpOne));

        List<Service> result = serviceService.getFeaturedServices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isFeatured()).isTrue();
    }

    @Test
    void shouldReturnAllServicesPageWhenThereAreNoFilters() {
        when(serviceRepository.search(null, null, null, pageRequest)).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        serviceService.getServicesPage(null, null, null, pageRequest);

        verify(serviceRepository).search(null, null, null, pageRequest);
    }

    // BGSI-302: brand + search box + active
    @Test
    void shouldFilterServicesPageByBrandTextAndActive() {
        when(serviceRepository.search(5L, "%one%", false, pageRequest)).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

        serviceService.getServicesPage(5L, "One", false, pageRequest);

        verify(serviceRepository).search(5L, "%one%", false, pageRequest);
    }

    @Test
    void shouldCreateServiceLinkedToItsBrand() {
        Service newService = new Service(99L, "BDP Kiosk", "Self-service kiosk", null);
        when(brandRepository.findById(5L)).thenReturn(Optional.of(bdp));
        when(serviceRepository.save(newService)).thenReturn(newService);

        Service result = serviceService.createService(newService, 5L);

        assertThat(result.getId()).isNull();
        assertThat(result.getBrand().getName()).isEqualTo("BDP");
    }

    @Test
    void shouldNotCreateServiceForUnknownBrand() {
        when(brandRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceService.createService(new Service(), 77L))
                .isInstanceOf(BrandNotFoundException.class);

        verify(serviceRepository, never()).save(any());
    }

    @Test
    void shouldUpdateAllFieldsAndBrand() {
        Service existing = new Service(1L, "Old name", "Old text", "old.jpg");
        Service changes = new Service(null, "BDP One", "New text", null);
        changes.setFeatured(true);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(5L)).thenReturn(Optional.of(bdp));
        when(serviceRepository.save(existing)).thenReturn(existing);

        Service result = serviceService.updateService(1L, changes, 5L);

        assertThat(result.getName()).isEqualTo("BDP One");
        assertThat(result.getImageUrl()).isNull();
        assertThat(result.isFeatured()).isTrue();
        assertThat(result.getBrand()).isEqualTo(bdp);
    }

    @Test
    void shouldThrowWhenUpdatingUnknownService() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceService.updateService(99L, new Service(), 5L))
                .isInstanceOf(ServiceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void shouldDeleteService() {
        Service service = new Service(1L, "BDP One", "Text", null);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));

        serviceService.deleteService(1L);

        verify(serviceRepository).delete(service);
    }

    // BGSI-309: activate / inactivate

    @Test
    void shouldReturnOnlyTheVisibleProductsOfABrand() {
        when(serviceRepository.findVisibleByBrandId(5L, pageRequest))
                .thenReturn(new PageImpl<>(List.of(new Service(1L, "BDP One", "Text", null))));

        assertThat(serviceService.getVisibleServicesPage(5L, pageRequest).getContent()).hasSize(1);
    }

    @Test
    void shouldInactivateService() {
        Service service = new Service(1L, "BDP One", "Text", null);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);

        Service result = serviceService.setServiceActive(1L, false);

        assertThat(result.isActive()).isFalse();
        verify(serviceRepository).save(service);
    }

    @Test
    void shouldActivateServiceAgain() {
        Service service = new Service(1L, "BDP One", "Text", null);
        service.setActive(false);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);

        assertThat(serviceService.setServiceActive(1L, true).isActive()).isTrue();
    }

    @Test
    void shouldThrowWhenChangingTheStateOfAnUnknownService() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceService.setServiceActive(99L, false))
                .isInstanceOf(ServiceNotFoundException.class);

        verify(serviceRepository, never()).save(any());
    }

    @Test
    void shouldCopyTheStateWhenUpdating() {
        Service existing = new Service(1L, "BDP One", "Text", null);
        Service changes = new Service(null, "BDP One", "Text", null);
        changes.setActive(false);
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(brandRepository.findById(5L)).thenReturn(Optional.of(bdp));
        when(serviceRepository.save(existing)).thenReturn(existing);

        assertThat(serviceService.updateService(1L, changes, 5L).isActive()).isFalse();
    }
}
