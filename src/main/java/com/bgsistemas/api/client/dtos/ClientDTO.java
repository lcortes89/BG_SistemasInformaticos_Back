package com.bgsistemas.api.client.dtos;

public class ClientDTO {

    private Long id;
    private String name;
    private String businessType;
    private String testimonial;
    private String logoUrl;
    private Integer rating;

    public ClientDTO() {
    }

    public ClientDTO(Long id, String name, String businessType, String testimonial,
                     String logoUrl, Integer rating) {
        this.id = id;
        this.name = name;
        this.businessType = businessType;
        this.testimonial = testimonial;
        this.logoUrl = logoUrl;
        this.rating = rating;
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
}