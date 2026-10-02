package com.landlord.android.feature.tenants;

/** Wire shape for POST /api/tenants/register - TenantController.RegisterRequest
 *  record (name, phone, email, nationalId, unitId, terms, deposit, password). */
public class TenantRegisterRequest {
    public String name;
    public String phone;
    public String email;
    public String nationalId;
    public Long unitId;
    public String terms;
    public Double deposit;
    public String password; // null - no tenant self-service login account created from the app in v1
}
