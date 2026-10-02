package com.landlord.android.feature.marketplace;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.google.gson.Gson;
import com.landlord.android.core.common.AppExecutors;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.sync.OpType;
import com.landlord.android.core.sync.PendingOperationDao;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncScheduler;
import com.landlord.android.core.sync.SyncState;
import java.util.List;

public class MarketplaceRepository {

    private static final String ENTITY_TYPE = "MARKETPLACE_REQUEST";

    private final Context appContext;
    private final MarketplaceRequestDao dao;
    private final PendingOperationDao opDao;
    private final Gson gson = new Gson();

    public MarketplaceRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.dao = db.marketplaceRequestDao();
        this.opDao = db.pendingOperationDao();
    }

    public LiveData<List<MarketplaceRequestEntity>> observeAll() {
        return dao.observeAll();
    }

    public void decide(String localId, String newStatus) {
        AppExecutors.DB.execute(() -> {
            MarketplaceRequestEntity entity = dao.findByLocalId(localId);
            if (entity == null) return;

            entity.status = newStatus;
            entity.sync.syncState = SyncState.PENDING_UPDATE;
            entity.sync.updatedLocallyAt = System.currentTimeMillis();
            dao.update(entity);

            UpdateStatusPayload payload = new UpdateStatusPayload();
            payload.status = newStatus;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = localId;
            op.opType = OpType.UPDATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }
}
