package com.bgsistemas.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users") // "user" is a reserved word in some databases
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Always stored hashed with BCrypt, never in plain text
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // BGSI-272: tokens issued before this moment are no longer valid (null = never changed)
    private Instant passwordChangedAt;

    // BGSI-276: two-factor authentication. The secret is encrypted (SecretCipher); null = never set up.
    // Wrapper types so the columns can be added to the existing table (old rows get null = off)
    private String totpSecret;
    private Boolean totpEnabled;
    // Last 30-second step used: the same code cannot be used twice
    private Long totpLastStep;

    // BGSI-277: null = ACTIVE (the admins created before this column existed)
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    private Instant lastLoginAt;

    public User() {
    }

    public User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(Instant passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public boolean isTotpEnabled() { return Boolean.TRUE.equals(totpEnabled); }
    public void setTotpEnabled(boolean totpEnabled) { this.totpEnabled = totpEnabled; }
    public Long getTotpLastStep() { return totpLastStep; }
    public void setTotpLastStep(Long totpLastStep) { this.totpLastStep = totpLastStep; }
    public UserStatus getStatus() { return status == null ? UserStatus.ACTIVE : status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public boolean isActive() { return getStatus() == UserStatus.ACTIVE; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
