package com.landlord.android.feature.properties;

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
import java.util.List;
import java.util.UUID;

public class UnitRepository {

    private static final String ENTITY_TYPE = "UNIT";
    private static final String PROPERTY_ENTITY_TYPE = "PROPERTY";

    private final Context appContext;
    private final UnitDao unitDao;
    private final PropertyDao propertyDao;
    private final PendingOperationDao opDao;
    private final Gson gson = new Gson();

    public UnitRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.unitDao = db.unitDao();
        this.propertyDao = db.propertyDao();
        this.opDao = db.pendingOperationDao();
    }

    public LiveData<List<UnitEntity>> observeByProperty(String propertyLocalId) {
        return unitDao.observeByProperty(propertyLocalId);
    }

    public void createUnit(String propertyLocalId, String unitNumber, double rent) {
        AppExecutors.DB.execute(() -> {
            UnitEntity entity = new UnitEntity();
            entity.localId = UUID.randomUUID().toString();
            entity.propertyLocalId = propertyLocalId;
            entity.unitNumber = unitNumber;
            entity.rent = rent;
            entity.status = "vacant";
            entity.createdAt = System.currentTimeMillis();
            entity.sync = SyncMetadata.freshlyCreated();

            unitDao.upsert(entity);

            UnitCreatePayload payload = new UnitCreatePayload();
            payload.propertyLocalId = propertyLocalId;
            payload.unitNumber = unitNumber;
            payload.rent = rent;
            payload.status = "vacant";

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = entity.localId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            // Dependency ordering: if the parent Property hasn't synced yet,
            // this Unit's create must wait for the Property's create op to
            // clear first (its server id doesn't exist yet to reference).
            PropertyEntity parent = propertyDao.findByLocalId(propertyLocalId);
            if (parent != null && parent.sync.syncState != SyncState.SYNCED) {
                PendingOperationEntity parentOp = opDao.findByEntityAndLocalId(PROPERTY_ENTITY_TYPE, propertyLocalId);
                if (parentOp != null) {
                    op.dependsOnOpId = parentOp.opId;
                }
            }

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }
}
