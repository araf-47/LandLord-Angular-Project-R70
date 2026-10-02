package com.landlord.android.feature.maintenance;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

@Entity(tableName = "maintenance_tickets")
public class MaintenanceTicketEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    public String unitLocalId;
    public String description;
    public String status = "pending";
    public Double cost;
    public String photoUrl;

    /** Local file path of a picked photo not yet (or just) uploaded - distinct
     *  from photoUrl, which is the server's URL once the upload succeeds. */
    public String localPhotoPath;

    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
