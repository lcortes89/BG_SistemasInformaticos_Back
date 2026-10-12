package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ServiceRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void shouldSaveAndFindService() {
        Service service = new Service(null, "Web Design", "Custom website design", "design.jpg");

        Service saved = serviceRepository.save(service);

        assertThat(saved.getId()).isNotNull();
        assertThat(serviceRepository.findById(saved.getId())).isPresent();
    }

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void shouldFindServicesByBrandId() {
        Brand bdp = brandRepository.save(
                new Brand(null, "BDP", "Hospitality software", "Software", "bdp.png", true));

        Service bdpNet = new Service(null, "BDP .Net", "Classic version", "bdp-net.jpg");
        bdpNet.setBrand(bdp);
        serviceRepository.save(bdpNet);

        List<Service> result = serviceRepository.findByBrandId(bdp.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("BDP .Net");
    }
}

    