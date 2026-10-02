package com.landlord.android.feature.maintenance;

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

public class MaintenanceSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "MAINTENANCE_TICKET";

    private final MaintenanceApiService api;
    private final MaintenanceTicketDao ticketDao;
    private final UnitDao unitDao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public MaintenanceSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(MaintenanceApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.ticketDao = db.maintenanceTicketDao();
        this.unitDao = db.unitDao();
        this.idMapper = new IdMapper(db.idMappingDao());
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        TicketCreatePayload payload = gson.fromJson(op.payloadJson, TicketCreatePayload.class);

        Long serverUnitId = idMapper.resolveServerId(payload.unitLocalId);
        if (serverUnitId == null) {
            throw new SyncException("parent unit not yet synced");
        }

        NewTicketRequest request = new NewTicketRequest();
        request.unitId = serverUnitId;
        request.description = payload.description;

        try {
            Response<MaintenanceTicketDto> response = api.create(request).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().id == null) {
                throw new SyncException("create failed: HTTP " + response.code());
            }

            long serverId = response.body().id;
            idMapper.record(ENTITY_TYPE, op.entityLocalId, serverId);

            MaintenanceTicketEntity entity = ticketDao.findByLocalId(op.entityLocalId);
            if (entity != null) {
                entity.sync.serverId = serverId;
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                ticketDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error creating ticket", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        TicketStatusPayload payload = gson.fromJson(op.payloadJson, TicketStatusPayload.class);

        MaintenanceTicketEntity entity = ticketDao.findByLocalId(op.entityLocalId);
        Long serverId = entity != null ? entity.sync.serverId : idMapper.resolveServerId(op.entityLocalId);
        if (serverId == null) {
            throw new SyncException("ticket not yet synced");
        }

        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest();
        request.status = payload.status;
        request.cost = payload.cost;
        request.bearer = "landlord";

        try {
            Response<MaintenanceTicketDto> response = api.updateStatus(serverId, request).execute();
            if (!response.isSuccessful()) {
                throw new SyncException("status update failed: HTTP " + response.code());
            }

            if (entity != null) {
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                ticketDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error updating ticket status", e);
        }
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("ticket delete not supported - no endpoint exists");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<MaintenanceTicketDto>> response = api.list().execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (MaintenanceTicketDto dto : response.body()) {
                if (dto.id == null) continue;

                String unitLocalId = null;
                if (dto.unitId != null) {
                    UnitEntity parentUnit = unitDao.findByServerId(dto.unitId);
                    if (parentUnit != null) unitLocalId = parentUnit.localId;
                }

                MaintenanceTicketEntity existing = ticketDao.findByServerId(dto.id);
                if (existing != null) {
                    if (existing.sync.syncState == SyncState.PENDING_UPDATE
                            || existing.sync.syncState == SyncState.PENDING_DELETE) {
                        continue;
                    }
                    applyDto(existing, dto, unitLocalId);
                    ticketDao.update(existing);
                } else {
                    MaintenanceTicketEntity entity = new MaintenanceTicketEntity();
                    entity.localId = UUID.randomUUID().toString();
                    applyDto(entity, dto, unitLocalId);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    ticketDao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling tickets", e);
        }
    }

    private void applyDto(MaintenanceTicketEntity entity, MaintenanceTicketDto dto, String unitLocalId) {
        entity.unitLocalId = unitLocalId;
        entity.description = dto.description;
        entity.status = dto.status;
        entity.cost = dto.cost;
        entity.photoUrl = dto.photoUrl;
    }
}
