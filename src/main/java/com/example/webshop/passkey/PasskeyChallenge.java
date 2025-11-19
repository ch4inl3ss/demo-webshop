package com.example.webshop.passkey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "passkey_challenges")
public class PasskeyChallenge {

    @Id
    @Column(length = 64)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PasskeyChallengeType type;

    @Column(nullable = false)
    private Long userId;

    @Lob
    @Column(nullable = false)
    private String optionsJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected PasskeyChallenge() {
    }

    public PasskeyChallenge(String id, PasskeyChallengeType type, Long userId, String optionsJson, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.userId = userId;
        this.optionsJson = optionsJson;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public PasskeyChallengeType getType() {
        return type;
    }

    public Long getUserId() {
        return userId;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
