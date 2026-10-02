package com.landlord.android.feature.expenses;

import android.content.Context;
import com.google.gson.Gson;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.network.NetworkModule;
import com.landlord.android.core.sync.EntitySyncHandler;
import com.landlord.android.core.sync.IdMapper;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncException;
import com.landlord.android.core.sync.SyncState;
import com.landlord.android.feature.properties.PropertyDao;
import com.landlord.android.feature.properties.PropertyEntity;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import retrofit2.Response;

public class ExpenseSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "EXPENSE";

    private final ExpenseApiService api;
    private final ExpenseDao expenseDao;
    private final PropertyDao propertyDao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public ExpenseSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(ExpenseApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.expenseDao = db.expenseDao();
        this.propertyDao = db.propertyDao();
        this.idMapper = new IdMapper(db.idMappingDao());
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        ExpenseCreatePayload payload = gson.fromJson(op.payloadJson, ExpenseCreatePayload.class);

        Long serverPropertyId = idMapper.resolveServerId(payload.propertyLocalId);
        if (serverPropertyId == null) {
            throw new SyncException("parent property not yet synced");
        }

        ExpenseCreateRequest request = new ExpenseCreateRequest();
        request.propertyId = serverPropertyId;
        request.category = payload.category;
        request.description = payload.description;
        request.amount = payload.amount;
        request.bearer = payload.bearer;

        try {
            Response<ExpenseDto> response = api.create(request).execute();
            if (!response.isSuccessful() || response.body() == null || response.body().id == null) {
                throw new SyncException("create failed: HTTP " + response.code());
            }

            long serverId = response.body().id;
            idMapper.record(ENTITY_TYPE, op.entityLocalId, serverId);

            ExpenseEntity entity = expenseDao.findByLocalId(op.entityLocalId);
            if (entity != null) {
                entity.sync.serverId = serverId;
                entity.sync.syncState = SyncState.SYNCED;
                entity.sync.lastSyncedAt = System.currentTimeMillis();
                expenseDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error creating expense", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("expense update not supported - no PUT endpoint exists server-side");
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("expense delete not supported yet");
    }

    @Override
    public void pullAll() throws SyncException {
        try {
            Response<List<ExpenseDto>> response = api.list().execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new SyncException("list failed: HTTP " + response.code());
            }

            for (ExpenseDto dto : response.body()) {
                if (dto.id == null) continue;

                String propertyLocalId = null;
                if (dto.propertyId != null) {
                    PropertyEntity parent = propertyDao.findByServerId(dto.propertyId);
                    if (parent != null) propertyLocalId = parent.localId;
                }

                ExpenseEntity existing = expenseDao.findByServerId(dto.id);
                if (existing != null) {
                    if (existing.sync.syncState == SyncState.PENDING_UPDATE
                            || existing.sync.syncState == SyncState.PENDING_DELETE) {
                        continue;
                    }
                    applyDto(existing, dto, propertyLocalId);
                    expenseDao.update(existing);
                } else {
                    ExpenseEntity entity = new ExpenseEntity();
                    entity.localId = UUID.randomUUID().toString();
                    applyDto(entity, dto, propertyLocalId);
                    entity.sync.serverId = dto.id;
                    entity.sync.syncState = SyncState.SYNCED;
                    entity.sync.lastSyncedAt = System.currentTimeMillis();
                    expenseDao.upsert(entity);
                }
            }
        } catch (IOException e) {
            throw new SyncException("network error pulling expenses", e);
        }
    }

    private void applyDto(ExpenseEntity entity, ExpenseDto dto, String propertyLocalId) {
        entity.propertyLocalId = propertyLocalId;
        entity.category = dto.category;
        entity.description = dto.description;
        entity.amount = dto.amount != null ? dto.amount : 0;
        entity.bearer = dto.bearer;
    }
}
