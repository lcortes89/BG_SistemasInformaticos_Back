package com.bgsistemas.api.setting;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class SiteSettingServiceImpl implements SiteSettingService {

    static final String HERO_IMAGE_KEY = "hero.image.url";

    private final SiteSettingRepository siteSettingRepository;

    public SiteSettingServiceImpl(SiteSettingRepository siteSettingRepository) {
        this.siteSettingRepository = siteSettingRepository;
    }

    @Override
    @Transactional(readOnly = true) // BGSI-326: only reads
    public Optional<String> getHeroImageUrl() {
        return siteSettingRepository.findById(HERO_IMAGE_KEY).map(SiteSetting::getValue);
    }

    @Override
    public String updateHeroImageUrl(String imageUrl) {
        // save() inserts the row the first time and updates it afterwards (same key)
        SiteSetting saved = siteSettingRepository.save(new SiteSetting(HERO_IMAGE_KEY, imageUrl.trim()));
        return saved.getValue();
    }

    @Override
    public void resetHeroImage() {
        siteSettingRepository.deleteById(HERO_IMAGE_KEY);
    }
}
