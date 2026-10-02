package com.landlord.android.feature.maintenance;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MaintenanceTicketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(MaintenanceTicketEntity entity);

    @Update
    void update(MaintenanceTicketEntity entity);

    @Query("SELECT * FROM maintenance_tickets WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<MaintenanceTicketEntity>> observeAll();

    @Query("SELECT * FROM maintenance_tickets WHERE localId = :localId LIMIT 1")
    MaintenanceTicketEntity findByLocalId(String localId);

    @Query("SELECT * FROM maintenance_tickets WHERE server_id = :serverId LIMIT 1")
    MaintenanceTicketEntity findByServerId(long serverId);
}
