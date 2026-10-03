package com.landlord.android.feature.tenants;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TenantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(TenantEntity entity);

    @Update
    void update(TenantEntity entity);

    @Query("SELECT * FROM tenants WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<TenantEntity>> observeAll();

    @Query("SELECT * FROM tenants WHERE deleted_locally = 0")
    List<TenantEntity> getAllSync();

    @Query("SELECT * FROM tenants WHERE localId = :localId LIMIT 1")
    TenantEntity findByLocalId(String localId);

    @Query("SELECT * FROM tenants WHERE localId = :localId LIMIT 1")
    LiveData<TenantEntity> observeByLocalId(String localId);

    @Query("SELECT * FROM tenants WHERE server_id = :serverId LIMIT 1")
    TenantEntity findByServerId(long serverId);
}
