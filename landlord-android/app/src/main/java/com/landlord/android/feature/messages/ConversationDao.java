package com.landlord.android.feature.messages;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ConversationEntity entity);

    @Update
    void update(ConversationEntity entity);

    @Query("SELECT * FROM conversations WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<ConversationEntity>> observeAll();

    @Query("SELECT * FROM conversations WHERE deleted_locally = 0 AND sync_state = 'SYNCED'")
    List<ConversationEntity> getAllSynced();

    @Query("SELECT * FROM conversations WHERE localId = :localId LIMIT 1")
    ConversationEntity findByLocalId(String localId);

    @Query("SELECT * FROM conversations WHERE server_id = :serverId LIMIT 1")
    ConversationEntity findByServerId(long serverId);
}
