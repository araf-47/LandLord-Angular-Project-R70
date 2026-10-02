package com.landlord.android.feature.messages;

import android.content.Context;
import com.google.gson.Gson;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.network.NetworkModule;
import com.landlord.android.core.sync.EntitySyncHandler;
import com.landlord.android.core.sync.IdMapper;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncException;
import com.landlord.android.core.sync.SyncState;
import com.landlord.android.feature.tenants.TenantDao;
import com.landlord.android.feature.tenants.TenantEntity;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class ConversationSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "CONVERSATION";

    private final MessagingApiService api;
    private final ConversationDao conversationDao;
    private final TenantDao tenantDao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public ConversationSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(MessagingApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.conversationDao = db.conversationDao();
        this.tenantDao = db.tenantDao();
        this.idMapper = new IdMapper(db.idMappingDao());
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        ConversationCreatePayload payload = gson.fromJson(op.payloadJson, ConversationCreatePayload.class);

        NewConversationRequest request = new NewConversationRequest();
        request.withName = payload.withName;
        if (payload.tenantLocalId != null) {
            request.tenantId = idMapper.resolveServerId(payload.tenantLocalId);
            if (request.tenantId == null) {
                throw new SyncException("parent tenant not yet synced");
            }
        }

        try {
            Response<ConversationDto> response = api.createConversation(request).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().id == null) {
                throw new SyncException("create failed: HTTP " + response.code());
            }

            long serverId = response.body().id;
            idMapper.record(ENTITY_TYPE, op.entityLocalId, serverId);

            ConversationEntity entity = conversationDao.findByLocalId(op.entityLocalId);
            if (entity != null) {
                entity.sync.serverId = serverId;
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                conversationDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error creating conversation", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("conversation update not supported");
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("conversation delete not supported");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<ConversationDto>> response = api.listConversations().execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (ConversationDto dto : response.body()) {
                if (dto.id == null) continue;

                String tenantLocalId = null;
                if (dto.tenantId != null) {
                    TenantEntity parentTenant = tenantDao.findByServerId(dto.tenantId);
                    if (parentTenant != null) tenantLocalId = parentTenant.localId;
                }

                ConversationEntity existing = conversationDao.findByServerId(dto.id);
                if (existing != null) {
                    applyDto(existing, dto, tenantLocalId);
                    conversationDao.update(existing);
                } else {
                    ConversationEntity entity = new ConversationEntity();
                    entity.localId = UUID.randomUUID().toString();
                    applyDto(entity, dto, tenantLocalId);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    conversationDao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling conversations", e);
        }
    }

    private void applyDto(ConversationEntity entity, ConversationDto dto, String tenantLocalId) {
        entity.tenantLocalId = tenantLocalId;
        entity.withName = dto.withName;
    }
}
