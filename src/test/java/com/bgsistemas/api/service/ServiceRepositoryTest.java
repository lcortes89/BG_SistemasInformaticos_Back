package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ServiceRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private EntityManager entityManager;

    private Service save(String name, Brand brand, boolean featured, boolean active) {
        Service service = new Service(null, name, name, null);
        service.setBrand(brand);
        service.setFeatured(featured);
        service.setActive(active);
        return serviceRepository.save(service);
    }

    private List<String> names(List<Service> services) {
        return services.stream().map(Service::getName).toList();
    }

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

    // BGSI-309: the public website only shows active products of active brands

    @Test
    void shouldHideInactiveProductsAndProductsOfInactiveBrands() {
        Brand active = brandRepository.save(new Brand(null, "BDP", "Software", "software", null, true));
        Brand hidden = brandRepository.save(new Brand(null, "Old brand", "Software", "software", null, false));
        save("Visible", active, true, true);
        save("Inactive product", active, true, false);
        save("Product of hidden brand", hidden, true, true);

        assertThat(names(serviceRepository.findVisible())).containsExactly("Visible");
        assertThat(names(serviceRepository.findVisibleFeatured())).containsExactly("Visible");
    }

    @Test
    void shouldShowFeaturedOnlyWhenTheProductIsFeatured() {
        Brand bdp = brandRepository.save(new Brand(null, "BDP", "Software", "software", null, true));
        save("Featured", bdp, true, true);
        save("Normal", bdp, false, true);

        assertThat(names(serviceRepository.findVisibleFeatured())).containsExactly("Featured");
        assertThat(names(serviceRepository.findVisible())).containsExactly("Featured", "Normal");
    }

    @Test
    void shouldTreatOldRowsWithoutStateAsActive() {
        Brand bdp = brandRepository.save(new Brand(null, "BDP", "Software", "software", null, true));
        Service old = save("Old row", bdp, false, true);
        entityManager.flush();
        // Rows saved before the column existed have NULL
        entityManager
                .createQuery("UPDATE Service s SET s.active = NULL WHERE s.id = :id")
                .setParameter("id", old.getId())
                .executeUpdate();
        entityManager.clear();

        assertThat(names(serviceRepository.findVisible())).containsExactly("Old row");
        assertThat(serviceRepository.findById(old.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void shouldPageOnlyTheVisibleProductsOfABrand() {
        Brand bdp = brandRepository.save(new Brand(null, "BDP", "Software", "software", null, true));
        save("BDP One", bdp, false, true);
        save("BDP Old", bdp, false, false);

        var page = serviceRepository.findVisibleByBrandId(bdp.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("BDP One");
    }

    @Test
    void shouldReturnNoProductsWhenTheBrandIsInactive() {
        Brand hidden = brandRepository.save(new Brand(null, "Hidden", "Software", "software", null, false));
        save("Product", hidden, false, true);

        assertThat(serviceRepository.findVisibleByBrandId(hidden.getId(), PageRequest.of(0, 10))).isEmpty();
    }
}
