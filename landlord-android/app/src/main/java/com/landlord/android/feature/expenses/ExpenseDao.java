package com.landlord.android.feature.expenses;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ExpenseEntity entity);

    @Update
    void update(ExpenseEntity entity);

    @Query("SELECT * FROM expenses WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<ExpenseEntity>> observeAll();

    @Query("SELECT * FROM expenses WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    List<ExpenseEntity> getAllSync();

    @Query("SELECT * FROM expenses WHERE localId = :localId LIMIT 1")
    ExpenseEntity findByLocalId(String localId);

    @Query("SELECT * FROM expenses WHERE server_id = :serverId LIMIT 1")
    ExpenseEntity findByServerId(long serverId);
}
