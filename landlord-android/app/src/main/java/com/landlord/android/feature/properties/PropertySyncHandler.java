package com.landlord.android.feature.properties;

import android.content.Context;
import com.google.gson.Gson;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.network.NetworkModule;
import com.landlord.android.core.sync.EntitySyncHandler;
import com.landlord.android.core.sync.IdMapper;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncException;
import com.landlord.android.core.sync.SyncState;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;

public class PropertySyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "PROPERTY";

    private final PropertyApiService api;
    private final PropertyDao dao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public PropertySyncHandler(Context appContext) {
        this.api = NetworkModule.propertyApi(appContext);
        this.dao = AppDatabase.getInstance(appContext).propertyDao();
        this.idMapper = new IdMapper(AppDatabase.getInstance(appContext).idMappingDao());
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        PropertyDto dto = gson.fromJson(op.payloadJson, PropertyDto.class);
        try {
            Response<PropertyDto> response = api.create(dto).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().id == null) {
                throw new SyncException("create failed: HTTP " + response.code());
            }

            long serverId = response.body().id;
            idMapper.record(ENTITY_TYPE, op.entityLocalId, serverId);

            PropertyEntity entity = dao.findByLocalId(op.entityLocalId);
            if (entity != null) {
                entity.sync.serverId = serverId;
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                dao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error creating property", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("property update not supported yet");
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("property delete not supported yet");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<PropertyDto>> response = api.list().execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (PropertyDto dto : response.body()) {
                if (dto.id == null) continue;

                PropertyEntity existing = dao.findByServerId(dto.id);
                if (existing != null) {
                    // don't clobber a locally-pending write with a stale pull
                    if (existing.sync.syncState == SyncState.PENDING_UPDATE
                            || existing.sync.syncState == SyncState.PENDING_DELETE) {
                        continue;
                    }
                    applyDto(existing, dto);
                    dao.update(existing);
                } else {
                    PropertyEntity entity = new PropertyEntity();
                    entity.localId = java.util.UUID.randomUUID().toString();
                    applyDto(entity, dto);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    dao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling properties", e);
        }
    }

    private void applyDto(PropertyEntity entity, PropertyDto dto) {
        entity.name = dto.name;
        entity.address = dto.address;
        entity.district = dto.district;
        entity.area = dto.area;
        entity.propertyType = dto.propertyType;
        entity.landlordId = dto.landlordId != null ? dto.landlordId : 0L;
    }
}
