package com.landlord.android.core.sync;

/** One implementation per feature module (PropertySyncHandler, UnitSyncHandler,
 *  ...). SyncWorker dispatches queued ops to the matching handler by entityType. */
public interface EntitySyncHandler {

    String entityType();

    void pushCreate(PendingOperationEntity op) throws SyncException;

    void pushUpdate(PendingOperationEntity op) throws SyncException;

    void pushDelete(PendingOperationEntity op) throws SyncException;

    /** Full-list fetch + reconcile against local Room tables (no pagination
     *  exists on any backend list endpoint, so this is always a complete sync). */
    void pullAll() throws SyncException;
}
