package com.bgsistemas.api.config;

import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DataSeederTest {

    @Autowired private BrandRepository brandRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private ClientRepository clientRepository;

    private DataSeeder seeder() {
        return new DataSeeder(brandRepository, serviceRepository, clientRepository);
    }

    @Test
    void shouldLoadRealCatalogWhenDatabaseIsEmpty() {
        seeder().run();

        assertThat(brandRepository.count()).isEqualTo(4);
        assertThat(serviceRepository.count()).isEqualTo(10);
        assertThat(serviceRepository.findByFeaturedTrue()).hasSize(3);
        assertThat(clientRepository.findByPublishedTrue()).hasSize(2);
    }

    @Test
    void shouldNotDuplicateDataWhenRunTwice() {
        seeder().run();
        seeder().run();

        assertThat(brandRepository.count()).isEqualTo(4);
        assertThat(serviceRepository.count()).isEqualTo(10);
        assertThat(clientRepository.count()).isEqualTo(2);
    }

    // BGSI-205: provisional product sheets, never written over the admin's texts
    @Test
    void shouldCompleteTheProductSheetsWithoutOverwritingTheAdmin() {
        seeder().run();
        Service bdpOne = serviceRepository.findAll().stream()
                .filter(service -> service.getName().equals("BDP One")).findFirst().orElseThrow();

        assertThat(bdpOne.getLongDescription()).isNotBlank();
        assertThat(bdpOne.getFeatures()).isNotEmpty();
        assertThat(bdpOne.getSectors()).isNotEmpty();

        bdpOne.setLongDescription("Text written by the admin");
        serviceRepository.save(bdpOne);
        seeder().run();

        assertThat(serviceRepository.findById(bdpOne.getId()).orElseThrow().getLongDescription())
                .isEqualTo("Text written by the admin");
    }
}
