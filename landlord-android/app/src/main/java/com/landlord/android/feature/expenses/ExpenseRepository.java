package com.landlord.android.feature.expenses;

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
import com.landlord.android.feature.properties.PropertyDao;
import com.landlord.android.feature.properties.PropertyEntity;
import java.util.List;
import java.util.UUID;

public class ExpenseRepository {

    private static final String ENTITY_TYPE = "EXPENSE";
    private static final String PROPERTY_ENTITY_TYPE = "PROPERTY";

    private final Context appContext;
    private final ExpenseDao expenseDao;
    private final PropertyDao propertyDao;
    private final PendingOperationDao opDao;
    private final Gson gson = new Gson();

    public ExpenseRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.expenseDao = db.expenseDao();
        this.propertyDao = db.propertyDao();
        this.opDao = db.pendingOperationDao();
    }

    public LiveData<List<ExpenseEntity>> observeAll() {
        return expenseDao.observeAll();
    }

    public LiveData<List<PropertyEntity>> properties() {
        return propertyDao.observeAll();
    }

    public void createExpense(String propertyLocalId, String category, String description, double amount, String bearer) {
        AppExecutors.DB.execute(() -> {
            ExpenseEntity entity = new ExpenseEntity();
            entity.localId = UUID.randomUUID().toString();
            entity.propertyLocalId = propertyLocalId;
            entity.category = category;
            entity.description = description;
            entity.amount = amount;
            entity.bearer = bearer;
            entity.createdAt = System.currentTimeMillis();
            entity.sync = SyncMetadata.freshlyCreated();

            expenseDao.upsert(entity);

            ExpenseCreatePayload payload = new ExpenseCreatePayload();
            payload.propertyLocalId = propertyLocalId;
            payload.category = category;
            payload.description = description;
            payload.amount = amount;
            payload.bearer = bearer;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = entity.localId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

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
