package com.memorylink.connection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "family_relationships", schema = "memorylink")
public class FamilyRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_a_id", nullable = false)
    private Long userAId;

    @Column(name = "user_b_id", nullable = false)
    private Long userBId;

    /** B 是 A 的谁（可空：未确认时为空）。 */
    @Column(name = "relation_a_to_b", length = 30)
    private String relationAToB;

    /** A 是 B 的谁（可空：未确认时为空）。 */
    @Column(name = "relation_b_to_a", length = 30)
    private String relationBToA;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "a_status", nullable = false, length = 20)
    private String aStatus = "PENDING";

    @Column(name = "b_status", nullable = false, length = 20)
    private String bStatus = "PENDING";

    @Column(name = "a_confirmed_at")
    private Instant aConfirmedAt;

    @Column(name = "b_confirmed_at")
    private Instant bConfirmedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = "ACTIVE";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserAId() {
        return userAId;
    }

    public void setUserAId(Long userAId) {
        this.userAId = userAId;
    }

    public Long getUserBId() {
        return userBId;
    }

    public void setUserBId(Long userBId) {
        this.userBId = userBId;
    }

    public String getRelationAToB() {
        return relationAToB;
    }

    public void setRelationAToB(String relationAToB) {
        this.relationAToB = relationAToB;
    }

    public String getRelationBToA() {
        return relationBToA;
    }

    public void setRelationBToA(String relationBToA) {
        this.relationBToA = relationBToA;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAStatus() {
        return aStatus;
    }

    public void setAStatus(String aStatus) {
        this.aStatus = aStatus;
    }

    public String getBStatus() {
        return bStatus;
    }

    public void setBStatus(String bStatus) {
        this.bStatus = bStatus;
    }

    public Instant getAConfirmedAt() {
        return aConfirmedAt;
    }

    public void setAConfirmedAt(Instant aConfirmedAt) {
        this.aConfirmedAt = aConfirmedAt;
    }

    public Instant getBConfirmedAt() {
        return bConfirmedAt;
    }

    public void setBConfirmedAt(Instant bConfirmedAt) {
        this.bConfirmedAt = bConfirmedAt;
    }
}
