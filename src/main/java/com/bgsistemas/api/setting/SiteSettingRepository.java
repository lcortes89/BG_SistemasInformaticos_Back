package com.bgsistemas.api.setting;

import org.springframework.data.jpa.repository.JpaRepository;

// The primary key is the setting name, e.g. "hero.image.url"
public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
}
