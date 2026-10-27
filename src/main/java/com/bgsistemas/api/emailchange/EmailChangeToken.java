package com.bgsistemas.api.emailchange;

import com.bgsistemas.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

// BGSI-299: "change my email" link sent to the new address. The email only changes when the link is opened.
// Like the reset links (BGSI-273), only the SHA-256 hash of the token is saved
@Entity
// BGSI-326: the nightly cleanup deletes the old links by their expiry date
@Table(indexes = @Index(name = "idx_email_change_expires", columnList = "expires_at"))
public class EmailChangeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String newEmail;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    // null = not used (nor cancelled) yet
    private Instant usedAt;

    protected EmailChangeToken() {
    }

    public EmailChangeToken(User user, String newEmail, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.user = user;
        this.newEmail = newEmail;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getNewEmail() { return newEmail; }
    public String getTokenHash() { return tokenHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
}
