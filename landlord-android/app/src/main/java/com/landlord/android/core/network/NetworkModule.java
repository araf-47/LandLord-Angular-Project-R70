package com.landlord.android.core.network;

import android.content.Context;
import com.landlord.android.BuildConfig;
import com.landlord.android.core.auth.AuthApiService;
import com.landlord.android.core.auth.TokenManager;
import com.landlord.android.feature.properties.PropertyApiService;
import com.landlord.android.feature.settings.SettingsApiService;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkModule {

    private static Retrofit retrofit;

    public static synchronized Retrofit getRetrofit(Context appContext) {
        if (retrofit == null) {
            TokenManager tokenManager = TokenManager.getInstance(appContext);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new BaseUrlInterceptor(appContext))
                    .addInterceptor(new AuthInterceptor(tokenManager))
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            // baseUrl here is just a well-formed placeholder Retrofit requires
            // at construction time - BaseUrlInterceptor rewrites every
            // request's actual scheme/host/port from ServerConfig, read fresh
            // per-call, so editing the server URL at runtime works even for
            // ApiService instances that were created long before the edit.
            retrofit = new Retrofit.Builder()
                    .baseUrl("http://localhost/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthApiService authApi(Context appContext) {
        return getRetrofit(appContext).create(AuthApiService.class);
    }

    public static PropertyApiService propertyApi(Context appContext) {
        return getRetrofit(appContext).create(PropertyApiService.class);
    }

    public static SettingsApiService settingsApi(Context appContext) {
        return getRetrofit(appContext).create(SettingsApiService.class);
    }
}
