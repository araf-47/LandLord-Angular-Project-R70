package com.landlord.android.feature.properties;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

@Entity(tableName = "properties")
public class PropertyEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    public String name;
    public String address;
    public String district;
    public String area;
    public String propertyType;
    public long landlordId;
    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
