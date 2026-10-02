package com.landlord.android.feature.marketplace;

import android.content.Context;
import com.google.gson.Gson;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.network.NetworkModule;
import com.landlord.android.core.sync.EntitySyncHandler;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncException;
import com.landlord.android.core.sync.SyncState;
import com.landlord.android.feature.properties.UnitDao;
import com.landlord.android.feature.properties.UnitEntity;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class MarketplaceSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "MARKETPLACE_REQUEST";

    private final MarketplaceApiService api;
    private final MarketplaceRequestDao dao;
    private final UnitDao unitDao;
    private final Gson gson = new Gson();

    public MarketplaceSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(MarketplaceApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.dao = db.marketplaceRequestDao();
        this.unitDao = db.unitDao();
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("creating marketplace requests from the landlord app is not supported");
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        UpdateStatusPayload payload = gson.fromJson(op.payloadJson, UpdateStatusPayload.class);
        MarketplaceRequestEntity entity = dao.findByLocalId(op.entityLocalId);
        if (entity == null || entity.sync.serverId == null) {
            throw new SyncException("marketplace request missing server id");
        }

        try {
            Response<MarketplaceRequestDto> response =
                    api.updateStatus(entity.sync.serverId, new DecisionRequest(payload.status)).execute();
            if (!response.isSuccessful()) {
                throw new SyncException("status update failed: HTTP " + response.code());
            }

            entity.sync.syncState = SyncState.SYNCED;
            entity.sync.lastSyncedAt = System.currentTimeMillis();
            dao.update(entity);
        } catch (IOException e) {
            throw new SyncException("network error updating marketplace request", e);
        }
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("marketplace request delete not supported - no endpoint exists");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<MarketplaceRequestDto>> response = api.list().execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (MarketplaceRequestDto dto : response.body()) {
                if (dto.id == null) continue;

                String unitLocalId = null;
                if (dto.unitId != null) {
                    UnitEntity parentUnit = unitDao.findByServerId(dto.unitId);
                    if (parentUnit != null) unitLocalId = parentUnit.localId;
                }

                MarketplaceRequestEntity existing = dao.findByServerId(dto.id);
                if (existing != null) {
                    if (existing.sync.syncState == SyncState.PENDING_UPDATE) {
                        continue; // local decision not yet pushed - don't clobber it
                    }
                    applyDto(existing, dto, unitLocalId);
                    dao.update(existing);
                } else {
                    MarketplaceRequestEntity entity = new MarketplaceRequestEntity();
                    entity.localId = UUID.randomUUID().toString();
                    applyDto(entity, dto, unitLocalId);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    dao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling marketplace requests", e);
        }
    }

    private void applyDto(MarketplaceRequestEntity entity, MarketplaceRequestDto dto, String unitLocalId) {
        entity.unitLocalId = unitLocalId;
        entity.applicantName = dto.applicantName;
        entity.message = dto.message;
        entity.status = dto.status;
    }
}
