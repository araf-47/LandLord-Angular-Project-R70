package com.landlord.android.feature.notifications;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.landlord.android.core.common.AppExecutors;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.sync.OpType;
import com.landlord.android.core.sync.PendingOperationDao;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncScheduler;
import com.landlord.android.core.sync.SyncState;
import java.util.List;

public class NotificationRepository {

    private static final String ENTITY_TYPE = "NOTIFICATION";

    private final Context appContext;
    private final NotificationDao dao;
    private final PendingOperationDao opDao;

    public NotificationRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.dao = db.notificationDao();
        this.opDao = db.pendingOperationDao();
    }

    public LiveData<List<NotificationEntity>> observeAll() {
        return dao.observeAll();
    }

    public void markRead(String localId) {
        AppExecutors.DB.execute(() -> {
            NotificationEntity entity = dao.findByLocalId(localId);
            if (entity == null || entity.read) return;

            entity.read = true;
            entity.sync.syncState = SyncState.PENDING_UPDATE;
            entity.sync.updatedLocallyAt = System.currentTimeMillis();
            dao.update(entity);

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = localId;
            op.opType = OpType.UPDATE;
            op.payloadJson = "{}";
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }
}
