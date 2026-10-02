package com.landlord.android.feature.maintenance;

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

public class MaintenanceRepository {

    private static final String ENTITY_TYPE = "MAINTENANCE_TICKET";
    private static final String PHOTO_ENTITY_TYPE = "MAINTENANCE_TICKET_PHOTO";
    private static final String UNIT_ENTITY_TYPE = "UNIT";

    private final Context appContext;
    private final MaintenanceTicketDao ticketDao;
    private final UnitDao unitDao;
    private final PendingOperationDao opDao;
    private final Gson gson = new Gson();

    public MaintenanceRepository(Context appContext) {
        this.appContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.ticketDao = db.maintenanceTicketDao();
        this.unitDao = db.unitDao();
        this.opDao = db.pendingOperationDao();
    }

    public LiveData<List<MaintenanceTicketEntity>> observeAll() {
        return ticketDao.observeAll();
    }

    public LiveData<List<UnitEntity>> units() {
        return unitDao.observeAll();
    }

    public void createTicket(String unitLocalId, String description) {
        AppExecutors.DB.execute(() -> {
            MaintenanceTicketEntity entity = new MaintenanceTicketEntity();
            entity.localId = UUID.randomUUID().toString();
            entity.unitLocalId = unitLocalId;
            entity.description = description;
            entity.status = "pending";
            entity.createdAt = System.currentTimeMillis();
            entity.sync = SyncMetadata.freshlyCreated();

            ticketDao.upsert(entity);

            TicketCreatePayload payload = new TicketCreatePayload();
            payload.unitLocalId = unitLocalId;
            payload.description = description;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = entity.localId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            UnitEntity parent = unitDao.findByLocalId(unitLocalId);
            if (parent != null && parent.sync.syncState != SyncState.SYNCED) {
                PendingOperationEntity parentOp = opDao.findByEntityAndLocalId(UNIT_ENTITY_TYPE, unitLocalId);
                if (parentOp != null) {
                    op.dependsOnOpId = parentOp.opId;
                }
            }

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }

    public void resolveTicket(String ticketLocalId, double cost) {
        AppExecutors.DB.execute(() -> {
            MaintenanceTicketEntity entity = ticketDao.findByLocalId(ticketLocalId);
            if (entity == null) return;

            entity.status = "resolved";
            entity.cost = cost;
            entity.sync.syncState = SyncState.PENDING_UPDATE;
            entity.sync.updatedLocallyAt = System.currentTimeMillis();
            ticketDao.update(entity);

            TicketStatusPayload payload = new TicketStatusPayload();
            payload.status = "resolved";
            payload.cost = cost;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = ENTITY_TYPE;
            op.entityLocalId = ticketLocalId;
            op.opType = OpType.UPDATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            // if the ticket itself hasn't synced yet, this update must wait
            // behind its own create op the same way a child waits on a parent.
            if (entity.sync.serverId == null) {
                PendingOperationEntity createOp = opDao.findByEntityAndLocalId(ENTITY_TYPE, ticketLocalId);
                if (createOp != null) {
                    op.dependsOnOpId = createOp.opId;
                }
            }

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }

    /** localFilePath is already a copy under app-private storage (picked
     *  content:// Uris don't survive process death/offline queueing, a real
     *  file path does). Deferred the same way a child waits on its parent -
     *  a ticket created offline has no server id yet to upload against. */
    public void attachPhoto(String ticketLocalId, String localFilePath) {
        AppExecutors.DB.execute(() -> {
            MaintenanceTicketEntity entity = ticketDao.findByLocalId(ticketLocalId);
            if (entity == null) return;

            entity.localPhotoPath = localFilePath;
            ticketDao.update(entity);

            PhotoUploadPayload payload = new PhotoUploadPayload();
            payload.ticketLocalId = ticketLocalId;
            payload.localFilePath = localFilePath;

            PendingOperationEntity op = new PendingOperationEntity();
            op.entityType = PHOTO_ENTITY_TYPE;
            op.entityLocalId = ticketLocalId;
            op.opType = OpType.CREATE;
            op.payloadJson = gson.toJson(payload);
            op.sequenceNumber = System.currentTimeMillis();
            op.enqueuedAt = System.currentTimeMillis();

            if (entity.sync.serverId == null) {
                PendingOperationEntity ticketCreateOp = opDao.findByEntityAndLocalId(ENTITY_TYPE, ticketLocalId);
                if (ticketCreateOp != null) {
                    op.dependsOnOpId = ticketCreateOp.opId;
                }
            }

            opDao.insert(op);
            SyncScheduler.requestImmediateSync(appContext);
        });
    }
}
