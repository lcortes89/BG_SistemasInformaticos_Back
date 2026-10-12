package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ServiceRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void shouldSaveAndFindService() {
        Service service = new Service(null, "Web Design", "Custom website design", "design.jpg");

        Service saved = serviceRepository.save(service);

        assertThat(saved.getId()).isNotNull();
        assertThat(serviceRepository.findById(saved.getId())).isPresent();
    }

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

    @Test
    void shouldFindOnlyFeaturedServices() {
        Service featured = new Service(null, "BDP One", "Multiplatform version", "bdp-one.jpg");
        featured.setFeatured(true);
        Service normal = new Service(null, "Support", "Technical support", "support.jpg");

        serviceRepository.save(featured);
        serviceRepository.save(normal);

        List<Service> result = serviceRepository.findByFeaturedTrue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("BDP One");
    }

    @Test
    void shouldCountServicesOfABrand() {
        Brand pudu = brandRepository.save(new Brand(null, "PUDU", "Robots", "Hardware", null, true));
        Brand empty = brandRepository.save(new Brand(null, "Empty", "No services", "Hardware", null, true));
        for (String name : List.of("BellaBot", "KettyBot")) {
            Service robot = new Service(null, name, "Service robot", null);
            robot.setBrand(pudu);
            serviceRepository.save(robot);
        }

        assertThat(serviceRepository.countByBrandId(pudu.getId())).isEqualTo(2);
        assertThat(serviceRepository.countByBrandId(empty.getId())).isZero();
    }
}
