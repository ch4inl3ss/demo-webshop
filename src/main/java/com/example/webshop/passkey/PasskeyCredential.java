package com.example.webshop.passkey;

import com.example.webshop.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "passkey_credentials")
public class PasskeyCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    /** Credential ID, base64url. */
    @Column(nullable = false, unique = true, length = 1400)
    private String credentialId;

    /** COSE-kodierter Public Key, base64url. */
    @Column(nullable = false, length = 4000)
    private String publicKeyCose;

    @Column(nullable = false)
    private long signatureCount;

    private String label;

    private Instant createdAt;

    private Instant lastUsedAt;

    protected PasskeyCredential() {
    }

    public PasskeyCredential(AppUser user, String credentialId, String publicKeyCose, long signatureCount, String label) {
        this.user = user;
        this.credentialId = credentialId;
        this.publicKeyCose = publicKeyCose;
        this.signatureCount = signatureCount;
        this.label = label;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getCredentialId() { return credentialId; }
    public String getPublicKeyCose() { return publicKeyCose; }
    public long getSignatureCount() { return signatureCount; }
    public void setSignatureCount(long signatureCount) { this.signatureCount = signatureCount; }
    public String getLabel() { return label; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
