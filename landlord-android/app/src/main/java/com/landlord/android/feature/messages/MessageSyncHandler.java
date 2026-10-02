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
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class MessageSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "MESSAGE";

    private final MessagingApiService api;
    private final MessageDao messageDao;
    private final ConversationDao conversationDao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public MessageSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(MessagingApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.messageDao = db.messageDao();
        this.conversationDao = db.conversationDao();
        this.idMapper = new IdMapper(db.idMappingDao());
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        MessageCreatePayload payload = gson.fromJson(op.payloadJson, MessageCreatePayload.class);

        Long serverConversationId = idMapper.resolveServerId(payload.conversationLocalId);
        if (serverConversationId == null) {
            throw new SyncException("parent conversation not yet synced");
        }

        NewMessageRequest request = new NewMessageRequest();
        request.senderRole = "landlord";
        request.text = payload.text;

        try {
            Response<MessageDto> response = api.sendMessage(serverConversationId, request).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().id == null) {
                throw new SyncException("send failed: HTTP " + response.code());
            }

            long serverId = response.body().id;
            idMapper.record(ENTITY_TYPE, op.entityLocalId, serverId);

            MessageEntity entity = messageDao.findByLocalId(op.entityLocalId);
            if (entity != null) {
                entity.sync.serverId = serverId;
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                messageDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error sending message", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("message update not supported");
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("message delete not supported");
    }

    @Override
    public void pullAll() throws SyncException {
        for (ConversationEntity conversation : conversationDao.getAllSynced()) {
            try {
                Response<List<MessageDto>> response = api.listMessages(conversation.sync.serverId).execute();
                if (!response.isSuccessful() || response.body() == null) continue;

                for (MessageDto dto : response.body()) {
                    if (dto.id == null) continue;

                    MessageEntity existing = messageDao.findByServerId(dto.id);
                    if (existing != null) {
                        applyDto(existing, dto, conversation.localId);
                        messageDao.update(existing);
                    } else {
                        MessageEntity entity = new MessageEntity();
                        entity.localId = UUID.randomUUID().toString();
                        applyDto(entity, dto, conversation.localId);
                        entity.sync.serverId = dto.id;
                        entity.sync.syncState = SyncState.SYNCED;
                        entity.sync.lastSyncedAt = System.currentTimeMillis();
                        messageDao.upsert(entity);
                    }
                }
            } catch (IOException e) {
                throw new SyncException("network error pulling messages", e);
            }
        }
    }

    private void applyDto(MessageEntity entity, MessageDto dto, String conversationLocalId) {
        entity.conversationLocalId = conversationLocalId;
        entity.senderRole = dto.senderRole;
        entity.text = dto.text;
        entity.read = dto.read != null && dto.read;
    }
}
