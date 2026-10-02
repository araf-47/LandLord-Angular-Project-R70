package com.landlord.android.feature.properties;

/** Mirrors landlord-backend's Property @Entity exactly - the backend has no
 *  separate request/response DTO, the entity itself is the wire shape. */
public class PropertyDto {
    public Long id;
    public String name;
    public String address;
    public String district;
    public String area;
    public String propertyType;
    public Long landlordId;
    public String createdAt;
}
