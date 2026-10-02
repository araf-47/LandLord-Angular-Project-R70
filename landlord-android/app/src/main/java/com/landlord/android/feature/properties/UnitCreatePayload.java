package com.landlord.android.feature.properties;

/** Outbox payload shape for a queued Unit create - carries the parent's
 *  LOCAL id (not yet necessarily resolved to a server id at enqueue time).
 *  UnitSyncHandler resolves propertyLocalId -> server propertyId via
 *  IdMapper only when the op is actually pushed. */
public class UnitCreatePayload {
    public String propertyLocalId;
    public String unitNumber;
    public double rent;
    public String status;
}
