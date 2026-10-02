package com.landlord.android.core.network;

import android.content.Context;
import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Rewrites every outgoing request's scheme/host/port to whatever
 *  ServerConfig currently holds, read fresh on every call. This - not
 *  rebuilding the Retrofit instance - is what makes the user-editable
 *  server URL actually live: several repositories/sync handlers cache their
 *  ApiService proxy for process lifetime, so a config change only reaches
 *  them if the rewrite happens per-request, not per-Retrofit-instance. */
public class BaseUrlInterceptor implements Interceptor {

    private final Context appContext;

    public BaseUrlInterceptor(Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        HttpUrl configured = HttpUrl.parse(ServerConfig.getBaseUrl(appContext));
        if (configured == null) {
            return chain.proceed(original);
        }

        HttpUrl rewritten = original.url().newBuilder()
                .scheme(configured.scheme())
                .host(configured.host())
                .port(configured.port())
                .build();

        return chain.proceed(original.newBuilder().url(rewritten).build());
    }
}
