package com.landlord.backend.unit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long propertyId;

    @NotBlank
    private String unitNumber;

    private Double rent;

    private String status = "vacant";

    private boolean adPaused = false;

    private String photoUrl;

    /** Set whenever status flips to "vacant"; cleared once occupied again.
     *  Phase 16.3's ad-expiry reminder job reads this to find stale ads. */
    private Instant vacantSince;

    /** One-shot guard so the ad-expiry reminder fires once per vacancy stretch,
     *  not every day the job runs. Cleared alongside vacantSince. */
    private Instant adReminderSentAt;

    public Long getId() {
        return id;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    public Double getRent() {
        return rent;
    }

    public void setRent(Double rent) {
        this.rent = rent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAdPaused() {
        return adPaused;
    }

    public void setAdPaused(boolean adPaused) {
        this.adPaused = adPaused;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Instant getVacantSince() {
        return vacantSince;
    }

    public void setVacantSince(Instant vacantSince) {
        this.vacantSince = vacantSince;
    }

    public Instant getAdReminderSentAt() {
        return adReminderSentAt;
    }

    public void setAdReminderSentAt(Instant adReminderSentAt) {
        this.adReminderSentAt = adReminderSentAt;
    }
}
