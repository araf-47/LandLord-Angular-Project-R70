package com.landlord.android.core.auth;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkManager;
import com.landlord.android.core.common.AppExecutors;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.network.NetworkModule;
import com.landlord.android.core.sync.SyncStatusRepository;
import com.landlord.android.feature.settings.LogoutAllRequest;
import com.landlord.android.feature.settings.SettingsApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Local logout must succeed even if offline - local data belongs to a
 *  specific landlord, so everything is wiped to avoid leaking it into the
 *  next account that logs in on this device. The server-side
 *  logout-all call is best-effort only, never blocks the local cleanup. */
public class LogoutHelper {

    public static void logout(Context context) {
        Context appContext = context.getApplicationContext();

        // Best-effort: invalidate server-side sessions if we're online. Fire
        // and forget - a failure here must never block local logout.
        SettingsApiService api = NetworkModule.settingsApi(appContext);
        api.logoutAll(new LogoutAllRequest()).enqueue(new Callback<com.landlord.android.core.network.ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<com.landlord.android.core.network.ApiResponse<Object>> call,
                                    @NonNull Response<com.landlord.android.core.network.ApiResponse<Object>> response) {
            }

            @Override
            public void onFailure(@NonNull Call<com.landlord.android.core.network.ApiResponse<Object>> call, @NonNull Throwable t) {
            }
        });

        WorkManager.getInstance(appContext).cancelAllWork();

        AppExecutors.DB.execute(() -> AppDatabase.getInstance(appContext).clearAllTables());

        TokenManager.getInstance(appContext).clear();
        SessionRepository.getInstance(appContext).clear();
        SyncStatusRepository.getInstance(appContext).clearNeedsReauth();
    }
}
