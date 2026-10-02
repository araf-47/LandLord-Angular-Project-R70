package com.landlord.android.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import com.landlord.android.BuildConfig;

/** Backend base URL, user-editable at runtime (Settings > Server) instead of
 *  baked into BuildConfig - dev LAN IP changes constantly as the dev machine
 *  moves between networks, and that shouldn't require a rebuild. */
public class ServerConfig {

    private static final String PREFS = "server_config_prefs";
    private static final String KEY_BASE_URL = "base_url";

    public static String getBaseUrl(Context context) {
        String stored = prefs(context).getString(KEY_BASE_URL, null);
        return stored != null ? stored : BuildConfig.BASE_URL;
    }

    public static void setBaseUrl(Context context, String url) {
        String normalized = url.trim();
        if (!normalized.endsWith("/")) normalized = normalized + "/";
        prefs(context).edit().putString(KEY_BASE_URL, normalized).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
