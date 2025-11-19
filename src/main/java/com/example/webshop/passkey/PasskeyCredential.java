package com.example.webshop.passkey;

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

@Entity
@Table(name = "passkey_credentials")
public class PasskeyCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String credentialId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Lob
    @Column(nullable = false)
    private byte[] publicKeyCose;

    @Column(nullable = false)
    private long signatureCount;

    protected PasskeyCredential() {
    }

    public PasskeyCredential(String credentialId, AppUser user, byte[] publicKeyCose, long signatureCount) {
        this.credentialId = credentialId;
        this.user = user;
        this.publicKeyCose = publicKeyCose;
        this.signatureCount = signatureCount;
    }

    public Long getId() {
        return id;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public AppUser getUser() {
        return user;
    }

    public byte[] getPublicKeyCose() {
        return publicKeyCose;
    }

    public long getSignatureCount() {
        return signatureCount;
    }

    public void setSignatureCount(long signatureCount) {
        this.signatureCount = signatureCount;
    }
}
