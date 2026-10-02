package com.landlord.android.core.sync;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface IdMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(IdMappingEntity mapping);

    @Nullable
    @Query("SELECT serverId FROM id_mappings WHERE localId = :localId")
    Long resolveServerId(String localId);
}
