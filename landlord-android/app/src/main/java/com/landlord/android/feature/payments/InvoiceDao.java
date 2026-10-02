package com.landlord.android.feature.payments;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface InvoiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(InvoiceEntity entity);

    @Update
    void update(InvoiceEntity entity);

    @Query("SELECT * FROM invoices WHERE deleted_locally = 0 ORDER BY createdAt DESC")
    LiveData<List<InvoiceEntity>> observeAll();

    @Query("SELECT * FROM invoices WHERE localId = :localId LIMIT 1")
    InvoiceEntity findByLocalId(String localId);

    @Query("SELECT * FROM invoices WHERE server_id = :serverId LIMIT 1")
    InvoiceEntity findByServerId(long serverId);
}
