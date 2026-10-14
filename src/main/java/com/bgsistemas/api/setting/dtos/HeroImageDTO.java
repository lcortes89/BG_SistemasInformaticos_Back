package com.bgsistemas.api.setting.dtos;

// Answer of GET /api/settings/hero and PUT /api/admin/settings/hero.
// imageUrl = null -> no photo chosen, the frontend shows its default one
public record HeroImageDTO(String imageUrl) {
}
