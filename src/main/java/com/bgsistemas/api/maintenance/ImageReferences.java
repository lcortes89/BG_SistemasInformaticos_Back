package com.bgsistemas.api.maintenance;

import com.bgsistemas.api.setting.SiteSettingRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// BGSI-327: the images the website still uses (logos of brands and clients, photos of products, hero photo).
// Only the file name is compared: the same image may be saved with a local or a Supabase URL
@Component
public class ImageReferences {

    private static final List<String> QUERIES = List.of(
            "SELECT b.logoUrl FROM Brand b WHERE b.logoUrl IS NOT NULL",
            "SELECT c.logoUrl FROM Client c WHERE c.logoUrl IS NOT NULL",
            "SELECT s.imageUrl FROM Service s WHERE s.imageUrl IS NOT NULL");

    private final EntityManager entityManager;
    private final SiteSettingRepository siteSettingRepository;

    public ImageReferences(EntityManager entityManager, SiteSettingRepository siteSettingRepository) {
        this.entityManager = entityManager;
        this.siteSettingRepository = siteSettingRepository;
    }

    @Transactional(readOnly = true)
    public Set<String> fileNamesInUse() {
        List<String> urls = new ArrayList<>();
        for (String query : QUERIES) {
            urls.addAll(entityManager.createQuery(query, String.class).getResultList());
        }
        // Every setting, not only the hero photo: better to keep an image than to lose one
        siteSettingRepository.findAll().forEach(setting -> urls.add(setting.getValue()));
        return urls.stream()
                .map(ImageReferences::fileName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    // ".../uploads/3f2a.png?v=2" -> "3f2a.png"
    static String fileName(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String path = url.split("[?#]", 2)[0];
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
