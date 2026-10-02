package com.landlord.android.core.auth;

import android.content.Context;
import android.content.SharedPreferences;

/** Caches the logged-in landlord's own numeric id (from /api/auth/me) since
 *  POST /api/properties and /api/units require landlordId/propertyId to be
 *  supplied explicitly in the request body - the backend does not infer it
 *  from the JWT. Not sensitive, plain SharedPreferences is fine. */
public class SessionRepository {

    private static final String PREFS = "session_prefs";
    private static final String KEY_LANDLORD_ID = "landlord_id";
    private static final String KEY_USERNAME = "username";

    private static SessionRepository instance;

    private final SharedPreferences prefs;

    private SessionRepository(Context appContext) {
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized SessionRepository getInstance(Context appContext) {
        if (instance == null) {
            instance = new SessionRepository(appContext.getApplicationContext());
        }
        return instance;
    }

    public void saveLandlordId(long landlordId) {
        prefs.edit().putLong(KEY_LANDLORD_ID, landlordId).apply();
    }

    public long getLandlordId() {
        return prefs.getLong(KEY_LANDLORD_ID, -1L);
    }

    public void saveUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
