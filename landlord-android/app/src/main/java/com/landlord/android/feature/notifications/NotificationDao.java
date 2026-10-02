package com.landlord.android.feature.notifications;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(NotificationEntity entity);

    @Update
    void update(NotificationEntity entity);

    @Query("SELECT * FROM notifications WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<NotificationEntity>> observeAll();

    @Query("SELECT * FROM notifications WHERE localId = :localId LIMIT 1")
    NotificationEntity findByLocalId(String localId);

    @Query("SELECT * FROM notifications WHERE server_id = :serverId LIMIT 1")
    NotificationEntity findByServerId(long serverId);
}
