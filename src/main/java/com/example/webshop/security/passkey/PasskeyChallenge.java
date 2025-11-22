package com.example.webshop.security.passkey;

import com.example.webshop.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "passkey_challenges")
public class PasskeyChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasskeyChallengeType type;

    @Column(nullable = false, length = 120)
    private String challenge;

    @Column
    private String credentialId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed;

    protected PasskeyChallenge() {
    }

    public PasskeyChallenge(AppUser user, PasskeyChallengeType type, String challenge, String credentialId, Instant expiresAt) {
        this.user = user;
        this.type = type;
        this.challenge = challenge;
        this.credentialId = credentialId;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.consumed = false;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public PasskeyChallengeType getType() {
        return type;
    }

    public String getChallenge() {
        return challenge;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void markConsumed() {
        this.consumed = true;
    }
}
