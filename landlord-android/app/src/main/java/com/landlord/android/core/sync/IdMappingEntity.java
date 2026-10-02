package com.landlord.android.core.sync;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "id_mappings")
public class IdMappingEntity {

    @PrimaryKey
    @NonNull
    public String localId;

    public String entityType;
    public long serverId;
}
