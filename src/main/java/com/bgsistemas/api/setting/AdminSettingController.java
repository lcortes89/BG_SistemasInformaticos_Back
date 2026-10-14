package com.bgsistemas.api.setting;

import com.bgsistemas.api.setting.dtos.HeroImageDTO;
import com.bgsistemas.api.setting.dtos.HeroImageRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Only ADMIN (SecurityConfig protects /api/admin/**)
@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingController {

    private final SiteSettingService siteSettingService;

    public AdminSettingController(SiteSettingService siteSettingService) {
        this.siteSettingService = siteSettingService;
    }

    // PUT /api/admin/settings/hero { "imageUrl": "http://..." } -> the new photo
    @PutMapping("/hero")
    public HeroImageDTO updateHeroImage(@Valid @RequestBody HeroImageRequestDTO request) {
        return new HeroImageDTO(siteSettingService.updateHeroImageUrl(request.imageUrl()));
    }

    // DELETE /api/admin/settings/hero -> 204, back to the default photo
    @DeleteMapping("/hero")
    public ResponseEntity<Void> resetHeroImage() {
        siteSettingService.resetHeroImage();
        return ResponseEntity.noContent().build();
    }
}
