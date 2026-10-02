package com.landlord.android.core.sync;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * One queued mutation against a syncable entity. Drained by SyncWorker in
 * sequenceNumber order; an op whose dependsOnOpId still exists in this
 * table is deferred to the next pass (handles Property-before-Unit,
 * Invoice-before-Payment, etc. without a hand-built topo-sort).
 */
@Entity(tableName = "pending_operations")
public class PendingOperationEntity {

    @PrimaryKey(autoGenerate = true)
    public long opId;

    public String entityType;
    public String entityLocalId;
    public OpType opType;
    public String payloadJson;
    public long sequenceNumber;
    public long enqueuedAt;
    public int attemptCount;

    @Nullable
    public String lastErrorMessage;

    @Nullable
    public Long dependsOnOpId;
}
