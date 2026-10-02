package com.landlord.android.feature.messages;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.google.gson.Gson;
import com.landlord.android.core.common.AppExecutors;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.sync.OpType;
import com.landlord.android.core.sync.PendingOperationDao;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncMetadata;
import com.landlord.android.core.sync.SyncScheduler;
import com.landlord.android.core.sync.SyncState;
import com.landlord.android.feature.tenants.TenantDao;
import com.landlord.android.feature.tenants.TenantEntity;
import java.util.List;
import java.util.UUID;

public class MessagingRepository {

    private static final String CONVERSATION_ENTITY_TYPE = "CONVERSATION";
    private static final String MESSAGE_ENTITY_TYPE = "MESSAGE";
    private static final String TENANT_ENTITY_TYPE = "TENANT";

    private final Context appContext;
    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final TenantDao tenantDao;
    private final PendingOperationDao opDao;
    private final Gson gson = new Gson();

    public MessagingRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.conversationDao = db.conversationDao();
        this.messageDao = db.messageDao();
        this.tenantDao = db.tenantDao();
        this.opDao = db.pendingOperationDao();
    }

    public LiveData<List<ConversationEntity>> observeConversations() {
        return conversationDao.observeAll();
    }

    public LiveData<List<MessageEntity>> observeMessages(String conversationLocalId) {
        return messageDao.observeByConversation(conversationLocalId);
    }

    public LiveData<List<TenantEntity>> tenants() {
        return tenantDao.observeAll();
    }

    public void createConversation(String tenantLocalId, String withName) {
        AppExecutors.DB.execute(() -> {
            ConversationEntity entity = new ConversationEntity();
            entity.localId = UUID.randomUUID().toString();
            entity.tenantLocalId = tenantLocalId;
            entity.withName = withName;
            entity.createdAt = System.currentTimeMillis();
            entity.sync = SyncMetadata.freshlyCreated();

            conversationDao.upsert(entity);

            ConversationCreatePayload payload = new ConversationCreatePayload();
            payload.tenantLocalId = tenantLocalId;
            payload.withName = withName;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = CONVERSATION_ENTITY_TYPE;
            op.entityLocalId = entity.localId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            if (tenantLocalId != null) {
                TenantEntity parent = tenantDao.findByLocalId(tenantLocalId);
                if (parent != null && parent.sync.syncState != SyncState.SYNCED) {
                    PendingOperationEntity parentOp = opDao.findByEntityAndLocalId(TENANT_ENTITY_TYPE, tenantLocalId);
                    if (parentOp != null) {
                        op.dependsOnOpId = parentOp.opId;
                    }
                }
            }

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }

    public void sendMessage(String conversationLocalId, String text) {
        AppExecutors.DB.execute(() -> {
            MessageEntity entity = new MessageEntity();
            entity.localId = UUID.randomUUID().toString();
            entity.conversationLocalId = conversationLocalId;
            entity.senderRole = "landlord";
            entity.text = text;
            entity.sentAt = System.currentTimeMillis();
            entity.sync = SyncMetadata.freshlyCreated();

            messageDao.upsert(entity);

            MessageCreatePayload payload = new MessageCreatePayload();
            payload.conversationLocalId = conversationLocalId;
            payload.text = text;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = MESSAGE_ENTITY_TYPE;
            op.entityLocalId = entity.localId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            ConversationEntity parent = conversationDao.findByLocalId(conversationLocalId);
            if (parent != null && parent.sync.syncState != SyncState.SYNCED) {
                PendingOperationEntity parentOp = opDao.findByEntityAndLocalId(CONVERSATION_ENTITY_TYPE, conversationLocalId);
                if (parentOp != null) {
                    op.dependsOnOpId = parentOp.opId;
                }
            }

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }
}
