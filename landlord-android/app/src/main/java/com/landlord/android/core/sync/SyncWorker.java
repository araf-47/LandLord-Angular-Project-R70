package com.landlord.android.core.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.landlord.android.core.auth.TokenManager;
import com.landlord.android.core.db.AppDatabase;
import java.util.List;

public class SyncWorker extends Worker {

    private static final int MAX_ATTEMPTS = 5;

    private final AppDatabase db;
    private final TokenManager tokenManager;
    private final SyncStatusRepository statusRepository;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = AppDatabase.getInstance(context);
        tokenManager = TokenManager.getInstance(context);
        statusRepository = SyncStatusRepository.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!tokenManager.hasTokens()) {
            return Result.success();
        }

        try {
            pushOutbox();
            pullAll();
            return Result.success();
        } catch (UnrecoverableAuthException e) {
            statusRepository.markNeedsReauth();
            return Result.failure();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void pushOutbox() throws SyncException {
        PendingOperationDao dao = db.pendingOperationDao();
        List<PendingOperationEntity> ops = dao.getAllOrderedBySequence();

        for (PendingOperationEntity op : ops) {
            if (op.dependsOnOpId != null && dao.countById(op.dependsOnOpId) > 0) {
                continue; // parent not yet synced, defer to next pass
            }

            EntitySyncHandler handler = SyncHandlerRegistry.get(op.entityType);
            if (handler == null) continue;

            try {
                switch (op.opType) {
                    case CREATE: handler.pushCreate(op); break;
                    case UPDATE: handler.pushUpdate(op); break;
                    case DELETE: handler.pushDelete(op); break;
                }
                dao.delete(op);
            } catch (SyncException e) {
                dao.recordFailure(op.opId, e.getMessage());
                if (op.attemptCount + 1 >= MAX_ATTEMPTS) {
                    // left in place, flagged for the Sync Issues screen
                }
            }
        }
    }

    private void pullAll() throws SyncException {
        for (EntitySyncHandler handler : SyncHandlerRegistry.all()) {
            handler.pullAll();
        }
    }

    public static class UnrecoverableAuthException extends RuntimeException {
    }
}
