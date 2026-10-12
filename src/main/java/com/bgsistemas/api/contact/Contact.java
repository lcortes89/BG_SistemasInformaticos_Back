package com.bgsistemas.api.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;

@Entity
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private String interestedService;

    @Column(length = 2000)
    private String message;

    private boolean gdprConsent;

    @Enumerated(EnumType.STRING)
    private ContactStatus status = ContactStatus.NEW;

    private LocalDateTime createdAt;

    // Admin-only note (used later in the dashboard)
    @Column(length = 1000)
    private String note;

    public Contact() {
    }

    public Contact(String name, String email, String phone, String interestedService,
                   String message, boolean gdprConsent) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.interestedService = interestedService;
        this.message = message;
        this.gdprConsent = gdprConsent;
    }

    // Runs automatically right before the first INSERT
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getInterestedService() { return interestedService; }
    public void setInterestedService(String interestedService) { this.interestedService = interestedService; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isGdprConsent() { return gdprConsent; }
    public void setGdprConsent(boolean gdprConsent) { this.gdprConsent = gdprConsent; }
    public ContactStatus getStatus() { return status; }
    public void setStatus(ContactStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}