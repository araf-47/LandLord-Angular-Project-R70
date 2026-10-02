package com.landlord.android.feature.payments;

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

public class InvoiceRepository {

    private static final String ENTITY_TYPE = "INVOICE";
    private static final String TENANT_ENTITY_TYPE = "TENANT";

    private final Context appContext;
    private final InvoiceDao invoiceDao;
    private final TenantDao tenantDao;
    private final PendingOperationDao opDao;
    private final Gson gson = new Gson();

    public InvoiceRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.invoiceDao = db.invoiceDao();
        this.tenantDao = db.tenantDao();
        this.opDao = db.pendingOperationDao();
    }

    public LiveData<List<InvoiceEntity>> observeAll() {
        return invoiceDao.observeAll();
    }

    public LiveData<List<TenantEntity>> tenants() {
        return tenantDao.observeAll();
    }

    public void generateInvoice(String tenantLocalId, double utilitiesTotal) {
        AppExecutors.DB.execute(() -> {
            InvoiceEntity entity = new InvoiceEntity();
            entity.localId = UUID.randomUUID().toString();
            entity.tenantLocalId = tenantLocalId;
            entity.utilitiesTotal = utilitiesTotal;
            entity.status = "unpaid";
            entity.createdAt = System.currentTimeMillis();
            entity.sync = SyncMetadata.freshlyCreated();

            invoiceDao.upsert(entity);

            InvoiceCreatePayload payload = new InvoiceCreatePayload();
            payload.tenantLocalId = tenantLocalId;
            payload.utilitiesTotal = utilitiesTotal;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = entity.localId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            TenantEntity parentTenant = tenantDao.findByLocalId(tenantLocalId);
            if (parentTenant != null && parentTenant.sync.syncState != SyncState.SYNCED) {
                PendingOperationEntity parentOp = opDao.findByEntityAndLocalId(TENANT_ENTITY_TYPE, tenantLocalId);
                if (parentOp != null) {
                    op.dependsOnOpId = parentOp.opId;
                }
            }

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }
}
