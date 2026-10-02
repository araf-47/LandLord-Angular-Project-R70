package com.landlord.android.feature.payments;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PaymentEntity entity);

    @Update
    void update(PaymentEntity entity);

    @Query("SELECT * FROM payments WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<PaymentEntity>> observeAll();

    @Query("SELECT * FROM payments WHERE localId = :localId LIMIT 1")
    PaymentEntity findByLocalId(String localId);

    @Query("SELECT * FROM payments WHERE server_id = :serverId LIMIT 1")
    PaymentEntity findByServerId(long serverId);
}
