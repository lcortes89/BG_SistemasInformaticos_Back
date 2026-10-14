package com.bgsistemas.api.setting;

import java.util.Optional;

public interface SiteSettingService {

    // Photo behind the hero of the public views; empty = use the default one of the frontend
    Optional<String> getHeroImageUrl();

    String updateHeroImageUrl(String imageUrl);

    // Back to the default photo
    void resetHeroImage();
}
