package com.landlord.android.feature.tenants;

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
import com.landlord.android.feature.properties.UnitDao;
import com.landlord.android.feature.properties.UnitEntity;
import java.util.List;
import java.util.UUID;

public class TenantRepository {

    private static final String ENTITY_TYPE = "TENANT";
    private static final String UNIT_ENTITY_TYPE = "UNIT";

    private final Context appContext;
    private final TenantDao tenantDao;
    private final UnitDao unitDao;
    private final PendingOperationDao opDao;
    private final Gson gson = new Gson();

    public TenantRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.tenantDao = db.tenantDao();
        this.unitDao = db.unitDao();
        this.opDao = db.pendingOperationDao();
    }

    public LiveData<List<TenantEntity>> observeAll() {
        return tenantDao.observeAll();
    }

    public LiveData<List<UnitEntity>> units() {
        return unitDao.observeAll();
    }

    public void registerTenant(String unitLocalId, String name, String phone,
                                String email, String nationalId, String terms, double deposit) {
        AppExecutors.DB.execute(() -> {
            TenantEntity entity = new TenantEntity();
            entity.localId = UUID.randomUUID().toString();
            entity.unitLocalId = unitLocalId;
            entity.name = name;
            entity.phone = phone;
            entity.email = email;
            entity.nationalId = nationalId;
            entity.status = "active";
            entity.createdAt = System.currentTimeMillis();
            entity.sync = SyncMetadata.freshlyCreated();

            tenantDao.upsert(entity);

            TenantCreatePayload payload = new TenantCreatePayload();
            payload.unitLocalId = unitLocalId;
            payload.name = name;
            payload.phone = phone;
            payload.email = email;
            payload.nationalId = nationalId;
            payload.terms = terms;
            payload.deposit = deposit;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = entity.localId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            UnitEntity parentUnit = unitDao.findByLocalId(unitLocalId);
            if (parentUnit != null && parentUnit.sync.syncState != SyncState.SYNCED) {
                PendingOperationEntity parentOp = opDao.findByEntityAndLocalId(UNIT_ENTITY_TYPE, unitLocalId);
                if (parentOp != null) {
                    op.dependsOnOpId = parentOp.opId;
                }
            }

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }
}
