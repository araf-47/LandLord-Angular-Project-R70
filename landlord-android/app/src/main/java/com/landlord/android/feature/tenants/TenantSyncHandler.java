package com.landlord.android.feature.tenants;

import android.content.Context;
import com.google.gson.Gson;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.network.NetworkModule;
import com.landlord.android.core.sync.EntitySyncHandler;
import com.landlord.android.core.sync.IdMapper;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncException;
import com.landlord.android.core.sync.SyncState;
import com.landlord.android.feature.properties.UnitDao;
import com.landlord.android.feature.properties.UnitEntity;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class TenantSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "TENANT";

    private final TenantApiService api;
    private final TenantDao tenantDao;
    private final UnitDao unitDao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public TenantSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(TenantApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.tenantDao = db.tenantDao();
        this.unitDao = db.unitDao();
        this.idMapper = new IdMapper(db.idMappingDao());
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        TenantCreatePayload payload = gson.fromJson(op.payloadJson, TenantCreatePayload.class);

        Long serverUnitId = idMapper.resolveServerId(payload.unitLocalId);
        if (serverUnitId == null) {
            throw new SyncException("parent unit not yet synced");
        }

        TenantRegisterRequest request = new TenantRegisterRequest();
        request.name = payload.name;
        request.phone = payload.phone;
        request.email = payload.email;
        request.nationalId = payload.nationalId;
        request.unitId = serverUnitId;
        request.terms = payload.terms;
        request.deposit = payload.deposit;
        request.password = null;

        try {
            Response<TenantDto> response = api.register(request).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().id == null) {
                throw new SyncException("register failed: HTTP " + response.code());
            }

            long serverId = response.body().id;
            idMapper.record(ENTITY_TYPE, op.entityLocalId, serverId);

            TenantEntity entity = tenantDao.findByLocalId(op.entityLocalId);
            if (entity != null) {
                entity.sync.serverId = serverId;
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                tenantDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error registering tenant", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("tenant update not supported yet");
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("tenant delete/move-out not supported yet");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<TenantDto>> response = api.list().execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (TenantDto dto : response.body()) {
                if (dto.id == null) continue;

                String unitLocalId = null;
                if (dto.unitId != null) {
                    UnitEntity parentUnit = unitDao.findByServerId(dto.unitId);
                    if (parentUnit != null) unitLocalId = parentUnit.localId;
                }

                TenantEntity existing = tenantDao.findByServerId(dto.id);
                if (existing != null) {
                    if (existing.sync.syncState == SyncState.PENDING_UPDATE
                            || existing.sync.syncState == SyncState.PENDING_DELETE) {
                        continue;
                    }
                    applyDto(existing, dto, unitLocalId);
                    tenantDao.update(existing);
                } else {
                    TenantEntity entity = new TenantEntity();
                    entity.localId = UUID.randomUUID().toString();
                    applyDto(entity, dto, unitLocalId);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    tenantDao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling tenants", e);
        }
    }

    private void applyDto(TenantEntity entity, TenantDto dto, String unitLocalId) {
        entity.unitLocalId = unitLocalId;
        entity.name = dto.name;
        entity.phone = dto.phone;
        entity.email = dto.email;
        entity.nationalId = dto.nationalId;
        entity.status = dto.status;
    }
}
