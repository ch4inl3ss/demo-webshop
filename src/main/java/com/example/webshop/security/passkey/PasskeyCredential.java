package com.example.webshop.security.passkey;

import com.example.webshop.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "passkey_credentials")
public class PasskeyCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false, unique = true)
    private String credentialId;

    @Lob
    @Column(nullable = false)
    private String publicKeyPem;

    @Column(nullable = false)
    private long signatureCounter;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PasskeyCredential() {
    }

    public PasskeyCredential(AppUser user, String credentialId, String publicKeyPem) {
        this.user = user;
        this.credentialId = credentialId;
        this.publicKeyPem = publicKeyPem;
        this.signatureCounter = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public long getSignatureCounter() {
        return signatureCounter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void bumpCounter() {
        this.signatureCounter = this.signatureCounter + 1;
        this.updatedAt = Instant.now();
    }
}
