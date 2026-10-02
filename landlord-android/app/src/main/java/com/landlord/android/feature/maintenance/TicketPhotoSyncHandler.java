package com.landlord.android.feature.maintenance;

import android.content.Context;
import com.google.gson.Gson;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.network.NetworkModule;
import com.landlord.android.core.sync.EntitySyncHandler;
import com.landlord.android.core.sync.IdMapper;
import com.landlord.android.core.sync.PendingOperationEntity;
import com.landlord.android.core.sync.SyncException;
import java.io.File;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

/** A ticket's photo upload is its own outbox entity type (not folded into
 *  MAINTENANCE_TICKET's CREATE/UPDATE) so its multipart payload shape never
 *  collides with the status-update payload shape on the same entity. */
public class TicketPhotoSyncHandler implements EntitySyncHandler {

    private static final String ENTITY_TYPE = "MAINTENANCE_TICKET_PHOTO";

    private final MaintenanceApiService api;
    private final MaintenanceTicketDao ticketDao;
    private final IdMapper idMapper;
    private final Gson gson = new Gson();

    public TicketPhotoSyncHandler(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(MaintenanceApiService.class);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.ticketDao = db.maintenanceTicketDao();
        this.idMapper = new IdMapper(db.idMappingDao());
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public void pushCreate(PendingOperationEntity op) throws SyncException {
        PhotoUploadPayload payload = gson.fromJson(op.payloadJson, PhotoUploadPayload.class);

        Long serverTicketId = idMapper.resolveServerId(payload.ticketLocalId);
        if (serverTicketId == null) {
            // falls back to the ticket's own serverId if it was never routed
            // through IdMapper (e.g. pulled from the server, not created locally)
            MaintenanceTicketEntity ticket = ticketDao.findByLocalId(payload.ticketLocalId);
            serverTicketId = ticket != null ? ticket.sync.serverId : null;
        }
        if (serverTicketId == null) {
            throw new SyncException("ticket not yet synced");
        }

        File file = new File(payload.localFilePath);
        if (!file.exists()) {
            throw new SyncException("photo file no longer exists on device");
        }

        RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/*"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

        try {
            Response<MaintenanceTicketDto> response = api.uploadPhoto(serverTicketId, part).execute();
            if (!response.isSuccessful()) {
                throw new SyncException("upload failed: HTTP " + response.code());
            }

            MaintenanceTicketEntity entity = ticketDao.findByLocalId(payload.ticketLocalId);
            if (entity != null && response.body() != null) {
                entity.photoUrl = response.body().photoUrl;
                ticketDao.update(entity);
            }
        } catch (IOException e) {
            throw new SyncException("network error uploading photo", e);
        }
    }

    @Override
    public void pushUpdate(PendingOperationEntity op) throws SyncException {
        throw new SyncException("photo update not supported");
    }

    @Override
    public void pushDelete(PendingOperationEntity op) throws SyncException {
        throw new SyncException("photo delete not supported");
    }

    @Override
    public void pullAll() throws SyncException {
        // nothing to pull - photoUrl comes along with MaintenanceTicketDto
        // already, via MaintenanceSyncHandler.pullAll()
    }
}
