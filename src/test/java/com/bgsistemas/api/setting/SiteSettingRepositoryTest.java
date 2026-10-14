package com.bgsistemas.api.setting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SiteSettingRepositoryTest {

    @Autowired
    private SiteSettingRepository siteSettingRepository;

    @Test
    void shouldInsertTheFirstTimeAndUpdateAfterwards() {
        siteSettingRepository.save(new SiteSetting("hero.image.url", "http://a.com/1.jpg"));
        siteSettingRepository.save(new SiteSetting("hero.image.url", "http://a.com/2.jpg"));

        assertThat(siteSettingRepository.count()).isEqualTo(1);
        assertThat(siteSettingRepository.findById("hero.image.url"))
                .get().extracting(SiteSetting::getValue).isEqualTo("http://a.com/2.jpg");
    }
}
