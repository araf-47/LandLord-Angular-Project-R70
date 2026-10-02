package com.landlord.android.core.sync;

import java.util.LinkedHashMap;
import java.util.Map;

/** Feature modules register their EntitySyncHandler here (e.g. in
 *  LandlordApp.onCreate once Phase B lands) so SyncWorker never needs to
 *  know about feature modules directly. */
public class SyncHandlerRegistry {

    private static final Map<String, EntitySyncHandler> handlers = new LinkedHashMap<>();

    public static void register(EntitySyncHandler handler) {
        handlers.put(handler.entityType(), handler);
    }

    public static EntitySyncHandler get(String entityType) {
        return handlers.get(entityType);
    }

    public static Iterable<EntitySyncHandler> all() {
        return handlers.values();
    }
}
