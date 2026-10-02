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
import java.util.UUID;
import retrofit2.Response;

public class UnitSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "UNIT";

    private final UnitApiService api;
    private final UnitDao unitDao;
    private final PropertyDao propertyDao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public UnitSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(UnitApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.unitDao = db.unitDao();
        this.propertyDao = db.propertyDao();
        this.idMapper = new IdMapper(db.idMappingDao());
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        UnitCreatePayload payload = gson.fromJson(op.payloadJson, UnitCreatePayload.class);

        Long serverPropertyId = idMapper.resolveServerId(payload.propertyLocalId);
        if (serverPropertyId == null) {
            // Shouldn't happen if dependsOnOpId gating worked - defer instead of failing hard.
            throw new SyncException("parent property not yet synced");
        }

        UnitDto dto = new UnitDto();
        dto.propertyId = serverPropertyId;
        dto.unitNumber = payload.unitNumber;
        dto.rent = payload.rent;
        dto.status = payload.status;
        dto.adPaused = false;

        try {
            Response<UnitDto> response = api.create(dto).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().id == null) {
                throw new SyncException("create failed: HTTP " + response.code());
            }

            long serverId = response.body().id;
            idMapper.record(ENTITY_TYPE, op.entityLocalId, serverId);

            UnitEntity entity = unitDao.findByLocalId(op.entityLocalId);
            if (entity != null) {
                entity.sync.serverId = serverId;
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                unitDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error creating unit", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("unit update not supported yet");
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("unit delete not supported yet");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<UnitDto>> response = api.list().execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (UnitDto dto : response.body()) {
                if (dto.id == null || dto.propertyId == null) continue;

                PropertyEntity parent = propertyDao.findByServerId(dto.propertyId);
                if (parent == null) {
                    // Parent property hasn't been pulled locally yet - catches
                    // up on a later pull cycle once PropertySyncHandler runs first.
                    continue;
                }

                UnitEntity existing = unitDao.findByServerId(dto.id);
                if (existing != null) {
                    if (existing.sync.syncState == SyncState.PENDING_UPDATE
                            || existing.sync.syncState == SyncState.PENDING_DELETE) {
                        continue;
                    }
                    applyDto(existing, dto, parent.localId);
                    unitDao.update(existing);
                } else {
                    UnitEntity entity = new UnitEntity();
                    entity.localId = UUID.randomUUID().toString();
                    applyDto(entity, dto, parent.localId);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    unitDao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling units", e);
        }
    }

    private void applyDto(UnitEntity entity, UnitDto dto, String parentLocalId) {
        entity.propertyLocalId = parentLocalId;
        entity.unitNumber = dto.unitNumber;
        entity.rent = dto.rent != null ? dto.rent : 0;
        entity.status = dto.status;
        entity.adPaused = dto.adPaused != null && dto.adPaused;
        entity.photoUrl = dto.photoUrl;
    }
}
