package com.bgsistemas.api.brand.dtos;

public class BrandDTO {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String logoUrl;

    public BrandDTO() {
    }

    public BrandDTO(Long id, String name, String description, String category, String logoUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.logoUrl = logoUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}