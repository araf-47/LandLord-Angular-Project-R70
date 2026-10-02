package com.landlord.android.feature.payments;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

@Entity(tableName = "payments")
public class PaymentEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    /** Local UUID of the owning InvoiceEntity. */
    public String invoiceLocalId;

    /** Local UUID of the TenantEntity (Payment carries tenantId directly on the wire too). */
    public String tenantLocalId;

    public double amount;
    public String method = "cash";
    public String status = "confirmed";
    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
