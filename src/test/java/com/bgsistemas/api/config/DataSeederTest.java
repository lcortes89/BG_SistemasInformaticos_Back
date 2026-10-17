package com.bgsistemas.api.config;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceRepository;
import com.bgsistemas.api.setting.SiteSettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DataSeederTest {

    @Autowired private BrandRepository brandRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private SiteSettingRepository siteSettingRepository;

    private DataSeeder seeder() {
        return new DataSeeder(brandRepository, serviceRepository, clientRepository, siteSettingRepository);
    }

    private Service byName(String name) {
        return serviceRepository.findAll().stream()
                .filter(service -> service.getName().equals(name)).findFirst().orElseThrow();
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

    // BGSI-208: every product gets its real short sheet, image and official page
    @Test
    void shouldGiveEveryProductItsRealContent() {
        seeder().run();

        assertThat(serviceRepository.findAll()).allSatisfy(service -> {
            assertThat(service.getDescription()).isNotBlank();
            assertThat(service.getFeatures()).hasSize(3);
            assertThat(service.getSectors()).isNotEmpty();
            assertThat(service.getImageUrl()).startsWith("https://");
            assertThat(service.getOfficialUrl()).startsWith("https://");
        });
        assertThat(byName("BDP One").getOfficialUrl()).isEqualTo("https://www.bdpcenter.com/bdp-one/");
    }

    // BGSI-208: a database created before keeps its products, renamed and updated
    @Test
    void shouldUpdateAnOldDatabaseOnceAndRenameThePuduRobot() {
        Brand pudu = brandRepository.save(new Brand(null, "PUDU", "Robots", "Hardware", null, true));
        Service oldRobot = new Service(null, "Robot de servicio PUDU", "Old text", "/images/services/default.png");
        oldRobot.setBrand(pudu);
        oldRobot.setLongDescription("Provisional text");
        serviceRepository.save(oldRobot);

        seeder().run();

        Service robot = serviceRepository.findById(oldRobot.getId()).orElseThrow();
        assertThat(robot.getName()).isEqualTo("Robot de reparto PuduBot 2");
        assertThat(robot.getImageUrl()).isEqualTo("https://cdn.pudutech.com/img_entertainment_827f906627.webp");
        assertThat(robot.getLongDescription()).isNull();
        assertThat(siteSettingRepository.existsById(DataSeeder.REAL_CATALOG_KEY)).isTrue();
    }

    // Applied only once: the admin's changes in the panel are never overwritten
    @Test
    void shouldNotOverwriteWhatTheAdminChangesLater() {
        seeder().run();
        Service bdpOne = byName("BDP One");
        bdpOne.setDescription("Text written by the admin");
        bdpOne.setFeatures(List.of("Admin feature"));
        serviceRepository.save(bdpOne);

        seeder().run();

        Service saved = serviceRepository.findById(bdpOne.getId()).orElseThrow();
        assertThat(saved.getDescription()).isEqualTo("Text written by the admin");
        assertThat(saved.getFeatures()).containsExactly("Admin feature");
    }
}
