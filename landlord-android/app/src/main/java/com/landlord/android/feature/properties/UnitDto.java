package com.landlord.android.feature.properties;

/** Mirrors landlord-backend's Unit @Entity (the wire shape - no separate
 *  request/response DTO server-side). */
public class UnitDto {
    public Long id;
    public Long propertyId;
    public String unitNumber;
    public Double rent;
    public String status;
    public Boolean adPaused;
    public String photoUrl;
}
