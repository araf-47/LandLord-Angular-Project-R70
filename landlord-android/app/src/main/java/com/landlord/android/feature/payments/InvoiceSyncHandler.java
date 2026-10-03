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
import com.landlord.android.feature.tenants.TenantDao;
import com.landlord.android.feature.tenants.TenantEntity;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class InvoiceSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "INVOICE";

    private final InvoiceApiService api;
    private final InvoiceDao invoiceDao;
    private final TenantDao tenantDao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public InvoiceSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(InvoiceApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
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
        InvoiceCreatePayload payload = gson.fromJson(op.payloadJson, InvoiceCreatePayload.class);

        Long serverTenantId = idMapper.resolveServerId(payload.tenantLocalId);
        if (serverTenantId == null) {
            throw new SyncException("parent tenant not yet synced");
        }

        InvoiceGenerateRequest request = new InvoiceGenerateRequest();
        request.tenantId = serverTenantId;
        request.utilitiesTotal = payload.utilitiesTotal;

        try {
            Response<InvoiceDto> response = api.generate(request).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().id == null) {
                throw new SyncException("generate failed: HTTP " + response.code());
            }

            InvoiceDto dto = response.body();
            long serverId = dto.id;
            idMapper.record(ENTITY_TYPE, op.entityLocalId, serverId);

            InvoiceEntity entity = invoiceDao.findByLocalId(op.entityLocalId);
            if (entity != null) {
                entity.period = dto.period;
                entity.rent = dto.rent;
                entity.prevUnpaidRolled = dto.prevUnpaidRolled;
                entity.amount = dto.amount;
                entity.balance = dto.balance;
                entity.status = dto.status;
                entity.dueDate = dto.dueDate;
                entity.sync.serverId = serverId;
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                invoiceDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error generating invoice", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("invoice update not supported yet");
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("invoice delete not supported yet");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<InvoiceDto>> response = api.list().execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (InvoiceDto dto : response.body()) {
                if (dto.id == null) continue;

                String tenantLocalId = null;
                if (dto.tenantId != null) {
                    TenantEntity parentTenant = tenantDao.findByServerId(dto.tenantId);
                    if (parentTenant != null) tenantLocalId = parentTenant.localId;
                }

                InvoiceEntity existing = invoiceDao.findByServerId(dto.id);
                if (existing != null) {
                    if (existing.sync.syncState == SyncState.PENDING_UPDATE
                            || existing.sync.syncState == SyncState.PENDING_DELETE) {
                        continue;
                    }
                    applyDto(existing, dto, tenantLocalId);
                    invoiceDao.update(existing);
                } else {
                    InvoiceEntity entity = new InvoiceEntity();
                    entity.localId = UUID.randomUUID().toString();
                    applyDto(entity, dto, tenantLocalId);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    invoiceDao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling invoices", e);
        }
    }

    private void applyDto(InvoiceEntity entity, InvoiceDto dto, String tenantLocalId) {
        entity.tenantLocalId = tenantLocalId;
        entity.period = dto.period;
        entity.rent = dto.rent;
        entity.utilitiesTotal = dto.utilitiesTotal != null ? dto.utilitiesTotal : 0;
        entity.prevUnpaidRolled = dto.prevUnpaidRolled;
        entity.amount = dto.amount;
        entity.balance = dto.balance;
        entity.status = dto.status;
        entity.dueDate = dto.dueDate;
    }
}
