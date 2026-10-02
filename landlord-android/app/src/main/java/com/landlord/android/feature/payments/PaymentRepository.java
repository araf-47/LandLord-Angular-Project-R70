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
import java.util.List;
import java.util.UUID;

public class PaymentRepository {

    private static final String ENTITY_TYPE = "PAYMENT";
    private static final String INVOICE_ENTITY_TYPE = "INVOICE";

    private final Context appContext;
    private final PaymentDao paymentDao;
    private final InvoiceDao invoiceDao;
    private final PendingOperationDao opDao;
    private final Gson gson = new Gson();

    public PaymentRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.paymentDao = db.paymentDao();
        this.invoiceDao = db.invoiceDao();
        this.opDao = db.pendingOperationDao();
    }

    public LiveData<List<PaymentEntity>> observeAll() {
        return paymentDao.observeAll();
    }

    public void recordPayment(String invoiceLocalId, String tenantLocalId, double amount, String method) {
        AppExecutors.DB.execute(() -> {
            PaymentEntity entity = new PaymentEntity();
            entity.localId = UUID.randomUUID().toString();
            entity.invoiceLocalId = invoiceLocalId;
            entity.tenantLocalId = tenantLocalId;
            entity.amount = amount;
            entity.method = method;
            entity.status = "confirmed";
            entity.createdAt = System.currentTimeMillis();
            entity.sync = SyncMetadata.freshlyCreated();

            paymentDao.upsert(entity);

            PaymentCreatePayload payload = new PaymentCreatePayload();
            payload.invoiceLocalId = invoiceLocalId;
            payload.tenantLocalId = tenantLocalId;
            payload.amount = amount;
            payload.method = method;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = entity.localId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            InvoiceEntity parentInvoice = invoiceDao.findByLocalId(invoiceLocalId);
            if (parentInvoice != null && parentInvoice.sync.syncState != SyncState.SYNCED) {
                PendingOperationEntity parentOp = opDao.findByEntityAndLocalId(INVOICE_ENTITY_TYPE, invoiceLocalId);
                if (parentOp != null) {
                    op.dependsOnOpId = parentOp.opId;
                }
            }

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }
}
