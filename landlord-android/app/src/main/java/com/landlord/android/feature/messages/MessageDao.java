package com.landlord.android.feature.messages;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(MessageEntity entity);

    @Update
    void update(MessageEntity entity);

    @Query("SELECT * FROM messages WHERE conversationLocalId = :conversationLocalId AND deleted_locally = 0 ORDER BY sentAt ASC")
    LiveData<List<MessageEntity>> observeByConversation(String conversationLocalId);

    @Query("SELECT * FROM messages WHERE localId = :localId LIMIT 1")
    MessageEntity findByLocalId(String localId);

    @Query("SELECT * FROM messages WHERE server_id = :serverId LIMIT 1")
    MessageEntity findByServerId(long serverId);
}
