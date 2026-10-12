package com.bgsistemas.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceServiceImplTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    @Test
    void shouldReturnAllServices() {
        Service service = new Service(1L, "Web Design", "Custom website design", "design.jpg");
        when(serviceRepository.findAll()).thenReturn(List.of(service));

        List<Service> result = serviceService.getAllServices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Web Design");
    }

    // NEW: returns only the services of the requested brand
    @Test
    void shouldReturnServicesByBrandId() {
        Service bdpNet = new Service(1L, "BDP .Net", "Classic version", "bdp-net.jpg");
        Service bdpOne = new Service(2L, "BDP One", "Multiplatform version", "bdp-one.jpg");
        when(serviceRepository.findByBrandId(5L)).thenReturn(List.of(bdpNet, bdpOne));

        List<Service> result = serviceService.getServicesByBrandId(5L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Service::getName).containsExactly("BDP .Net", "BDP One");
    }
}