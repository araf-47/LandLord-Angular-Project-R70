package com.landlord.android.feature.tenants;

/** Outbox payload for a queued tenant registration - carries the parent
 *  Unit's LOCAL id, resolved to a server id only when the op is pushed. */
public class TenantCreatePayload {
    public String unitLocalId;
    public String name;
    public String phone;
    public String email;
    public String nationalId;
    public String terms;
    public double deposit;
}
