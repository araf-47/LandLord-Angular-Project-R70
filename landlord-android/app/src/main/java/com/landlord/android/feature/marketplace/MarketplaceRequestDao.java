package com.landlord.android.feature.marketplace;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MarketplaceRequestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(MarketplaceRequestEntity entity);

    @Update
    void update(MarketplaceRequestEntity entity);

    @Query("SELECT * FROM marketplace_requests WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<MarketplaceRequestEntity>> observeAll();

    @Query("SELECT * FROM marketplace_requests WHERE localId = :localId LIMIT 1")
    MarketplaceRequestEntity findByLocalId(String localId);

    @Query("SELECT * FROM marketplace_requests WHERE server_id = :serverId LIMIT 1")
    MarketplaceRequestEntity findByServerId(long serverId);
}
