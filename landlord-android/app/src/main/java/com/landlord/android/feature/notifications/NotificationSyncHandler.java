package com.landlord.android.feature.notifications;

import android.content.Context;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.network.NetworkModule;
import com.landlord.android.core.sync.EntitySyncHandler;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncException;
import com.landlord.android.core.sync.SyncState;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class NotificationSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "NOTIFICATION";

    private final NotificationApiService api;
    private final NotificationDao dao;

    public NotificationSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(NotificationApiService.class);
        this.dao = AppDatabase.getInstance(appContext).notificationDao();
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("creating notifications from the app is not supported");
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        NotificationEntity entity = dao.findByLocalId(op.entityLocalId);
        if (entity == null || entity.sync.serverId == null) {
            throw new SyncException("notification missing server id");
        }

        try {
            Response<Void> response = api.markRead(entity.sync.serverId).execute();
            if (!response.isSuccessful()) {
                throw new SyncException("mark-read failed: HTTP " + response.code());
            }

            entity.sync.syncState = SyncState.SYNCED;
            entity.sync.lastSyncedAt = System.currentTimeMillis();
            dao.update(entity);
        } catch (IOException e) {
            throw new SyncException("network error marking notification read", e);
        }
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("notification delete not supported yet");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<NotificationDto>> response = api.list("landlord").execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (NotificationDto dto : response.body()) {
                if (dto.id == null) continue;

                NotificationEntity existing = dao.findByServerId(dto.id);
                if (existing != null) {
                    if (existing.sync.syncState == SyncState.PENDING_UPDATE) {
                        continue; // local "mark read" not yet pushed
                    }
                    applyDto(existing, dto);
                    dao.update(existing);
                } else {
                    NotificationEntity entity = new NotificationEntity();
                    entity.localId = UUID.randomUUID().toString();
                    applyDto(entity, dto);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    dao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling notifications", e);
        }
    }

    private void applyDto(NotificationEntity entity, NotificationDto dto) {
        entity.type = dto.type;
        entity.title = dto.title;
        entity.body = dto.body;
        entity.read = dto.read != null && dto.read;
    }
}
