package com.barivara.backend.listing;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The public-facing ad. Denormalizes address/district/area/rent off the owner's
 * property/unit at creation time (same simplification style as LandLord's invoice
 * snapshotting) so search/filter never needs a join. `source` is "owner" for
 * BariVara-native listings (real `ownerId`, no `landlordUnitId`) or
 * "landlord-linked" for ones synced in from the LandLord backend (Phase 15's
 * VacancyAdSync — real `landlordUnitId`, `ownerId` stays null since no BariVara
 * owner account created it).
 */
@Entity
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;

    private Long unitId;

    /** LandLord's own `Unit.id` — set only for source="landlord-linked" listings,
     *  the correlation key Phase 15's sync endpoints look up by. */
    private Long landlordUnitId;

    private String photoUrl;

    @NotBlank
    private String source = "owner";

    @NotBlank
    private String title;

    @NotBlank
    private String address;

    @NotBlank
    private String district;

    @NotBlank
    private String area;

    @NotBlank
    private String propertyType;

    @NotNull
    private Double rent;

    @NotBlank
    private String status = "active";

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Long getLandlordUnitId() {
        return landlordUnitId;
    }

    public void setLandlordUnitId(Long landlordUnitId) {
        this.landlordUnitId = landlordUnitId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
