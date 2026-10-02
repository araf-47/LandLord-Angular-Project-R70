package com.landlord.android.core.auth;

import android.content.Context;
import android.content.SharedPreferences;

/** Local app-unlock convenience layer, independent of backend token
 *  validity - gates cold start only, never blocks sync or local data use. */
public class BiometricLockPrefs {

    private static final String PREFS = "biometric_lock_prefs";
    private static final String KEY_ENABLED = "enabled";

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
