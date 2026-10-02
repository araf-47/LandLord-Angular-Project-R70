package com.landlord.android.feature.properties;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.google.gson.Gson;
import com.landlord.android.core.auth.SessionRepository;
import com.landlord.android.core.common.AppExecutors;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.sync.OpType;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncMetadata;
import com.landlord.android.core.sync.SyncScheduler;
import java.util.List;
import java.util.UUID;

/** Every mutating method is local-first: write Room immediately, enqueue an
 *  outbox op, return - no direct Retrofit call in the mutation path. Actual
 *  network push happens later in PropertySyncHandler, dispatched by
 *  SyncWorker. */
public class PropertyRepository {

    private static final String ENTITY_TYPE = "PROPERTY";

    private final Context appContext;
    private final PropertyDao dao;
    private final Gson gson = new Gson();

    public PropertyRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        this.dao = AppDatabase.getInstance(appContext).propertyDao();
    }

    public LiveData<List<PropertyEntity>> observeAll() {
        return dao.observeAll();
    }

    public void createProperty(String name, String address, String district,
                                String area, String propertyType) {
        AppExecutors.DB.execute(() -> {
            PropertyEntity entity = new PropertyEntity();
            entity.localId = UUID.randomUUID().toString();
            entity.name = name;
            entity.address = address;
            entity.district = district;
            entity.area = area;
            entity.propertyType = propertyType;
            entity.landlordId = SessionRepository.getInstance(appContext).getLandlordId();
            entity.createdAt = System.currentTimeMillis();
            entity.sync = SyncMetadata.freshlyCreated();

            dao.upsert(entity);

            PropertyDto dto = new PropertyDto();
            dto.name = entity.name;
            dto.address = entity.address;
            dto.district = entity.district;
            dto.area = entity.area;
            dto.propertyType = entity.propertyType;
            dto.landlordId = entity.landlordId;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = entity.localId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(dto);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            AppDatabase.getInstance(appContext).pendingOperationDao().insert(op);

            SyncScheduler.requestImmediateSync(appContext);
        });
    }
}
