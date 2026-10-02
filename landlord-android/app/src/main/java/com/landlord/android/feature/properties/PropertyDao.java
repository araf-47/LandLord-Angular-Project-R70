package com.landlord.android.feature.properties;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PropertyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PropertyEntity entity);

    @Update
    void update(PropertyEntity entity);

    @Query("SELECT * FROM properties WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<PropertyEntity>> observeAll();

    @Query("SELECT * FROM properties WHERE localId = :localId LIMIT 1")
    PropertyEntity findByLocalId(String localId);

    @Query("SELECT * FROM properties WHERE localId = :localId LIMIT 1")
    LiveData<PropertyEntity> observeByLocalId(String localId);

    @Query("SELECT * FROM properties WHERE server_id = :serverId LIMIT 1")
    PropertyEntity findByServerId(long serverId);

    @Query("SELECT localId FROM properties WHERE deleted_locally = 0 AND sync_state = 'SYNCED'")
    List<String> allSyncedLocalIds();

    @Query("UPDATE properties SET deleted_locally = 1 WHERE localId = :localId")
    void tombstone(String localId);
}
