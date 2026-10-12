package com.bgsistemas.api.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String businessType;

    @Column(length = 1000)
    private String testimonial;

    private String logoUrl;
    private Integer rating;
    private boolean published;

    public Client() {
    }

    public Client(Long id, String name, String businessType, String testimonial,
                  String logoUrl, Integer rating, boolean published) {
        this.id = id;
        this.name = name;
        this.businessType = businessType;
        this.testimonial = testimonial;
        this.logoUrl = logoUrl;
        this.rating = rating;
        this.published = published;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getTestimonial() { return testimonial; }
    public void setTestimonial(String testimonial) { this.testimonial = testimonial; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}