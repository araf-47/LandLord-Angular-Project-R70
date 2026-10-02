package com.landlord.android.feature.payments;

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

public class PaymentSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "PAYMENT";

    private final PaymentApiService api;
    private final PaymentDao paymentDao;
    private final InvoiceDao invoiceDao;
    private final com.landlord.android.feature.tenants.TenantDao tenantDao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public PaymentSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(PaymentApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.paymentDao = db.paymentDao();
        this.invoiceDao = db.invoiceDao();
        this.tenantDao = db.tenantDao();
        this.idMapper = new IdMapper(db.idMappingDao());
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        PaymentCreatePayload payload = gson.fromJson(op.payloadJson, PaymentCreatePayload.class);

        Long serverInvoiceId = idMapper.resolveServerId(payload.invoiceLocalId);
        Long serverTenantId = idMapper.resolveServerId(payload.tenantLocalId);
        if (serverInvoiceId == null || serverTenantId == null) {
            throw new SyncException("parent invoice/tenant not yet synced");
        }

        RecordPaymentRequest request = new RecordPaymentRequest();
        request.tenantId = serverTenantId;
        request.invoiceId = serverInvoiceId;
        request.amount = payload.amount;
        request.method = payload.method;

        try {
            Response<PaymentDto> response = api.create(request).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().id == null) {
                throw new SyncException("record payment failed: HTTP " + response.code());
            }

            long serverId = response.body().id;
            idMapper.record(ENTITY_TYPE, op.entityLocalId, serverId);

            PaymentEntity entity = paymentDao.findByLocalId(op.entityLocalId);
            if (entity != null) {
                entity.sync.serverId = serverId;
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                paymentDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error recording payment", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("payment update not supported");
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("payment delete not supported");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<PaymentDto>> response = api.list().execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (PaymentDto dto : response.body()) {
                if (dto.id == null) continue;

                String invoiceLocalId = null;
                if (dto.invoiceId != null) {
                    InvoiceEntity parentInvoice = invoiceDao.findByServerId(dto.invoiceId);
                    if (parentInvoice != null) invoiceLocalId = parentInvoice.localId;
                }

                String tenantLocalId = null;
                if (dto.tenantId != null) {
                    com.landlord.android.feature.tenants.TenantEntity parentTenant = tenantDao.findByServerId(dto.tenantId);
                    if (parentTenant != null) tenantLocalId = parentTenant.localId;
                }

                PaymentEntity existing = paymentDao.findByServerId(dto.id);
                if (existing != null) {
                    if (existing.sync.syncState == SyncState.PENDING_UPDATE
                            || existing.sync.syncState == SyncState.PENDING_DELETE) {
                        continue;
                    }
                    applyDto(existing, dto, invoiceLocalId, tenantLocalId);
                    paymentDao.update(existing);
                } else {
                    PaymentEntity entity = new PaymentEntity();
                    entity.localId = UUID.randomUUID().toString();
                    applyDto(entity, dto, invoiceLocalId, tenantLocalId);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    paymentDao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling payments", e);
        }
    }

    private void applyDto(PaymentEntity entity, PaymentDto dto, String invoiceLocalId, String tenantLocalId) {
        entity.invoiceLocalId = invoiceLocalId;
        entity.tenantLocalId = tenantLocalId;
        entity.amount = dto.amount != null ? dto.amount : 0;
        entity.method = dto.method;
        entity.status = dto.status;
    }
}
