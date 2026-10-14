package com.bgsistemas.api.setting;

import com.bgsistemas.api.setting.dtos.HeroImageDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Public: the website reads its settings without logging in
@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SiteSettingService siteSettingService;

    public SettingController(SiteSettingService siteSettingService) {
        this.siteSettingService = siteSettingService;
    }

    // GET /api/settings/hero -> { "imageUrl": "http://..." } or { "imageUrl": null }
    @GetMapping("/hero")
    public HeroImageDTO getHeroImage() {
        return new HeroImageDTO(siteSettingService.getHeroImageUrl().orElse(null));
    }
}
