package com.landlord.android.feature.properties;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface UnitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UnitEntity entity);

    @Update
    void update(UnitEntity entity);

    @Query("SELECT * FROM units WHERE propertyLocalId = :propertyLocalId AND deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<UnitEntity>> observeByProperty(String propertyLocalId);

    @Query("SELECT * FROM units WHERE localId = :localId LIMIT 1")
    UnitEntity findByLocalId(String localId);

    @Query("SELECT * FROM units WHERE server_id = :serverId LIMIT 1")
    UnitEntity findByServerId(long serverId);

    @Query("SELECT * FROM units WHERE deleted_locally = 0 ORDER BY unitNumber ASC")
    LiveData<List<UnitEntity>> observeAll();
}
