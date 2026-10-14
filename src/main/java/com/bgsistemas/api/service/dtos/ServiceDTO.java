package com.bgsistemas.api.service.dtos;

import java.util.ArrayList;
import java.util.List;

public class ServiceDTO {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Long brandId;      // NEW
    private String brandName;  // NEW

    // BGSI-205: full product sheet
    private String longDescription;
    private List<String> features = new ArrayList<>();
    private List<String> sectors = new ArrayList<>();
    private String officialUrl;

    public ServiceDTO() {
    }

    // Original constructor: kept so existing tests (e.g. ServiceControllerTest) keep working
    public ServiceDTO(Long id, String name, String description, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    // NEW constructor including brand data
    public ServiceDTO(Long id, String name, String description, String imageUrl,
                      Long brandId, String brandName) {
        this(id, name, description, imageUrl);
        this.brandId = brandId;
        this.brandName = brandName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // NEW
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    // BGSI-205
    public String getLongDescription() { return longDescription; }
    public void setLongDescription(String longDescription) { this.longDescription = longDescription; }
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
    public List<String> getSectors() { return sectors; }
    public void setSectors(List<String> sectors) { this.sectors = sectors; }
    public String getOfficialUrl() { return officialUrl; }
    public void setOfficialUrl(String officialUrl) { this.officialUrl = officialUrl; }
}
