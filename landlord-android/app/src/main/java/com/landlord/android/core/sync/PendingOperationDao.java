package com.landlord.android.core.sync;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PendingOperationDao {

    @Insert
    long insert(PendingOperationEntity op);

    @Update
    void update(PendingOperationEntity op);

    @Delete
    void delete(PendingOperationEntity op);

    @Query("SELECT * FROM pending_operations ORDER BY sequenceNumber ASC")
    List<PendingOperationEntity> getAllOrderedBySequence();

    @Query("SELECT * FROM pending_operations WHERE entityType = :entityType AND entityLocalId = :entityLocalId LIMIT 1")
    PendingOperationEntity findByEntityAndLocalId(String entityType, String entityLocalId);

    @Query("SELECT COUNT(*) FROM pending_operations WHERE opId = :opId")
    int countById(long opId);

    @Query("UPDATE pending_operations SET attemptCount = attemptCount + 1, lastErrorMessage = :error WHERE opId = :opId")
    void recordFailure(long opId, String error);
}
