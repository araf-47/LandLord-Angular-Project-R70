package com.landlord.android.feature.expenses;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.landlord.android.core.sync.SyncMetadata;

/** No PUT/PATCH exists server-side for expenses - create/list/delete only.
 *  Don't offer an edit UI for this entity. */
@Entity(tableName = "expenses")
public class ExpenseEntity {

    @PrimaryKey
    @NonNull
    public String localId = "";

    public String propertyLocalId;
    public String category;
    public String description;
    public double amount;
    public String bearer = "landlord";
    public long createdAt;

    @Embedded
    public SyncMetadata sync = new SyncMetadata();
}
