package com.landlord.android.core.sync;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/** Tracks whether the outbox is blocked on a dead session. A sync failure
 *  due to an unrecoverable 401/403 sets needsReauth instead of forcing
 *  logout, so the app stays usable offline while a banner prompts re-login. */
public class SyncStatusRepository {

    private static final String PREFS = "sync_status_prefs";
    private static final String KEY_NEEDS_REAUTH = "needs_reauth";

    private static SyncStatusRepository instance;

    private final SharedPreferences prefs;
    private final MutableLiveData<Boolean> needsReauth;

    private SyncStatusRepository(Context appContext) {
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        needsReauth = new MutableLiveData<>(prefs.getBoolean(KEY_NEEDS_REAUTH, false));
    }

    public static synchronized SyncStatusRepository getInstance(Context appContext) {
        if (instance == null) {
            instance = new SyncStatusRepository(appContext.getApplicationContext());
        }
        return instance;
    }

    public LiveData<Boolean> needsReauth() {
        return needsReauth;
    }

    public void markNeedsReauth() {
        prefs.edit().putBoolean(KEY_NEEDS_REAUTH, true).apply();
        needsReauth.postValue(true);
    }

    public void clearNeedsReauth() {
        prefs.edit().putBoolean(KEY_NEEDS_REAUTH, false).apply();
        needsReauth.postValue(false);
    }
}
