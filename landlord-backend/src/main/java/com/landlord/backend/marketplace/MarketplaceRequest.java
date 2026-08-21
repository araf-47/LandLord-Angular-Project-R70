package com.landlord.backend.marketplace;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class MarketplaceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long unitId;

    private String applicantName;

    /** Set when the applicant is already a LandLord tenant (internal transfer
     *  request). Left unset for external BariVara marketplace applicants. */
    private Long tenantId;

    /** Set for requests synced in from BariVara (Phase 15's BookingRequestSync) —
     *  BariVara's own tenant id, unrelated to this app's `tenantId`. Null for
     *  requests created locally (internal transfer, walk-in via marketplace UI). */
    private Long barivaraTenantId;

    private String message;

    private String status = "pending";

    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getBarivaraTenantId() {
        return barivaraTenantId;
    }

    public void setBarivaraTenantId(Long barivaraTenantId) {
        this.barivaraTenantId = barivaraTenantId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
