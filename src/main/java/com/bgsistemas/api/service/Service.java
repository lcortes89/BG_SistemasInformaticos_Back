package com.bgsistemas.api.service;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.globals.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String imageUrl;

    // NEW: marks the service to be shown on the home page (false by default)
    private boolean featured;

    // BGSI-205: full product sheet
    @Column(length = 2000)
    private String longDescription;

    // "What it does": 3 or 4 short sentences, saved one per line
    @Convert(converter = StringListConverter.class)
    @Column(length = 1000)
    private List<String> features = new ArrayList<>();

    // "Ideal for": bars, restaurants...
    @Convert(converter = StringListConverter.class)
    @Column(length = 500)
    private List<String> sectors = new ArrayList<>();

    // Link to the official product page (maker website)
    private String officialUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    public Service() {
    }

    public Service(Long id, String name, String description, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // NEW
    public boolean isFeatured() {
        return featured;
    }

    // NEW
    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    // Never null, even if the column was empty (old rows)
    public List<String> getFeatures() {
        return features == null ? new ArrayList<>() : features;
    }

    public void setFeatures(List<String> features) {
        this.features = features == null ? new ArrayList<>() : new ArrayList<>(features);
    }

    public List<String> getSectors() {
        return sectors == null ? new ArrayList<>() : sectors;
    }

    public void setSectors(List<String> sectors) {
        this.sectors = sectors == null ? new ArrayList<>() : new ArrayList<>(sectors);
    }

    public String getOfficialUrl() {
        return officialUrl;
    }

    public void setOfficialUrl(String officialUrl) {
        this.officialUrl = officialUrl;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }
}
