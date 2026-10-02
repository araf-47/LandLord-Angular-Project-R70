package com.landlord.android.feature.marketplace;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

/** Only pulled from the server (a landlord-side Android app doesn't
 *  originate marketplace requests - those come from applicants/BariVara) -
 *  so every row here always has a serverId by the time it exists locally.
 *  The only local mutation is approve/reject (an UPDATE op). */
@Entity(tableName = "marketplace_requests")
public class MarketplaceRequestEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    public String unitLocalId;
    public String applicantName;
    public String message;
    public String status = "pending";
    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
