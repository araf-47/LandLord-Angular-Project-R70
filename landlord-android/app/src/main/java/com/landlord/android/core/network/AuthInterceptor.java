package com.landlord.android.core.network;

import androidx.annotation.NonNull;
import com.landlord.android.core.auth.TokenManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Mirrors auth.interceptor.ts: attaches Authorization + x-refresh-token on
 *  every request, and persists a rotated access token echoed back in the
 *  x-access-token response header. Not a classic 401-refresh pattern -
 *  Parts/auth/CommonConstants.java: ACCESS_TOKEN_HEADER="x-access-token",
 *  REFRESH_TOKEN_HEADER="x-refresh-token". */
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String accessToken = tokenManager.getAccessToken();
        String refreshToken = tokenManager.getRefreshToken();

        Request.Builder builder = original.newBuilder();
        if (accessToken != null) {
            builder.addHeader("Authorization", "Bearer " + accessToken);
            if (refreshToken != null) {
                builder.addHeader("x-refresh-token", refreshToken);
            }
        }

        Response response = chain.proceed(builder.build());

        String rotated = response.header("x-access-token");
        if (rotated != null) {
            tokenManager.updateAccessToken(rotated);
        }

        return response;
    }
}
