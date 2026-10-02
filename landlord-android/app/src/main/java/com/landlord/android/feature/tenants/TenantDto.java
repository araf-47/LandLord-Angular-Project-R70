package com.landlord.android.feature.tenants;

/** Mirrors landlord-backend's Tenant @Entity - returned by both GET /api/tenants
 *  and POST /api/tenants/register. */
public class TenantDto {
    public Long id;
    public String name;
    public String phone;
    public String email;
    public String nationalId;
    public Long unitId;
    public String status;
    public Long authUserId;
}
