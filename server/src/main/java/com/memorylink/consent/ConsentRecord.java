package com.memorylink.consent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "consent_records", schema = "memorylink")
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loved_one_id", nullable = false)
    private Long lovedOneId;

    @Column(name = "consent_type", nullable = false, length = 30)
    private String consentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consentor_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> consentorIds = new ArrayList<>();

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "file_key", length = 255)
    private String fileKey;

    @Column(nullable = false, length = 20)
    private String status = "VALID";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = "VALID";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLovedOneId() {
        return lovedOneId;
    }

    public void setLovedOneId(Long lovedOneId) {
        this.lovedOneId = lovedOneId;
    }

    public String getConsentType() {
        return consentType;
    }

    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }

    public List<Long> getConsentorIds() {
        return consentorIds;
    }

    public void setConsentorIds(List<Long> consentorIds) {
        this.consentorIds = consentorIds;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
