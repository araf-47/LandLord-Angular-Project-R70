package com.landlord.android.core.sync;

import androidx.room.ColumnInfo;

/**
 * Embedded in every syncable Room entity. localId is always the entity's
 * real primary key (set client-side on creation) so offline-created rows
 * never need to wait on a server-assigned id. serverId is populated once
 * a create is acknowledged and is used only by the sync layer for
 * correlation - local queries/joins always use localId.
 */
public class SyncMetadata {

    @ColumnInfo(name = "server_id")
    public Long serverId;

    @ColumnInfo(name = "sync_state")
    public SyncState syncState = SyncState.PENDING_CREATE;

    @ColumnInfo(name = "updated_locally_at")
    public long updatedLocallyAt;

    @ColumnInfo(name = "last_synced_at")
    public long lastSyncedAt;

    @ColumnInfo(name = "deleted_locally")
    public boolean deletedLocally;

    public static SyncMetadata freshlyCreated() {
        SyncMetadata meta = new SyncMetadata();
        meta.syncState = SyncState.PENDING_CREATE;
        meta.updatedLocallyAt = System.currentTimeMillis();
        return meta;
    }

    public static SyncMetadata fromServer(long serverId) {
        SyncMetadata meta = new SyncMetadata();
        meta.serverId = serverId;
        meta.syncState = SyncState.SYNCED;
        meta.lastSyncedAt = System.currentTimeMillis();
        return meta;
    }
}
