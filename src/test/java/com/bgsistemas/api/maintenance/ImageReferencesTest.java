package com.bgsistemas.api.maintenance;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.client.Client;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceRepository;
import com.bgsistemas.api.setting.SiteSetting;
import com.bgsistemas.api.setting.SiteSettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

// BGSI-327: which images the website still uses
@DataJpaTest
@Import(ImageReferences.class)
class ImageReferencesTest {

    @Autowired
    private ImageReferences references;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private SiteSettingRepository siteSettingRepository;

    @Test
    void collectsTheFileNamesOfLogosPhotosAndSettings() {
        brandRepository.save(new Brand(null, "BDP", "TPV", "Software", "http://localhost:8080/uploads/brand.png", true));
        brandRepository.save(new Brand(null, "No logo", "TPV", "Software", null, true));
        clientRepository.save(new Client(null, "Bar", "Bar", "Great", "https://abc.supabase.co/storage/v1/object/public/images/client.webp", 5, true));
        serviceRepository.save(new Service(null, "BDP One", "TPV", "http://localhost:8080/uploads/product.jpg?v=2"));
        siteSettingRepository.save(new SiteSetting("hero.image.url", "https://abc.supabase.co/storage/v1/object/public/images/hero.png"));

        assertThat(references.fileNamesInUse()).contains("brand.png", "client.webp", "product.jpg", "hero.png");
    }

    @Test
    void takesTheFileNameOutOfAnyUrl() {
        assertThat(ImageReferences.fileName("http://localhost:8080/uploads/a.png")).isEqualTo("a.png");
        assertThat(ImageReferences.fileName("https://x.supabase.co/storage/v1/object/public/images/b.png#top")).isEqualTo("b.png");
        assertThat(ImageReferences.fileName("c.png")).isEqualTo("c.png");
        assertThat(ImageReferences.fileName(" ")).isNull();
        assertThat(ImageReferences.fileName(null)).isNull();
    }
}
