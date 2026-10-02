package com.landlord.android.feature.payments;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

@Entity(tableName = "invoices")
public class InvoiceEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    /** Local UUID of the owning TenantEntity. */
    public String tenantLocalId;

    public String period;
    public Double rent;
    public double utilitiesTotal;
    public Double amount;
    public Double balance;
    public String status = "unpaid";
    public String dueDate;
    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
