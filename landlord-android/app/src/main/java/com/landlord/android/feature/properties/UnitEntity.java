package com.landlord.android.feature.properties;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

@Entity(tableName = "units")
public class UnitEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    /** Always the local UUID of the owning PropertyEntity - never the
     *  server id. Resolved to a server id only at outgoing-request
     *  serialization time by UnitSyncHandler via IdMapper. */
    public String propertyLocalId;

    public String unitNumber;
    public double rent;
    public String status = "vacant";
    public boolean adPaused;
    public String photoUrl;
    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
