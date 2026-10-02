package com.landlord.android.core.sync;

import androidx.annotation.Nullable;

/** Thin wrapper over IdMappingDao, used by EntitySyncHandlers to resolve a
 *  child entity's parent foreign key (still in local-UUID space) to the
 *  real server id, only at outgoing-request serialization time. */
public class IdMapper {

    private final IdMappingDao dao;

    public IdMapper(IdMappingDao dao) {
        this.dao = dao;
    }

    public void record(String entityType, String localId, long serverId) {
        IdMappingEntity mapping = new IdMappingEntity();
        mapping.localId = localId;
        mapping.entityType = entityType;
        mapping.serverId = serverId;
        dao.upsert(mapping);
    }

    @Nullable
    public Long resolveServerId(String localId) {
        return dao.resolveServerId(localId);
    }
}
