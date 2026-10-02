package com.landlord.android.core.auth;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.landlord.android.core.network.ApiResponse;
import com.landlord.android.core.network.NetworkModule;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.landlord.android.core.common.Result;

public class AuthRepository {

    private final AuthApiService api;
    private final TokenManager tokenManager;
    private final SessionRepository sessionRepository;

    public AuthRepository(Context appContext) {
        this.api = NetworkModule.authApi(appContext);
        this.tokenManager = TokenManager.getInstance(appContext);
        this.sessionRepository = SessionRepository.getInstance(appContext);
    }

    public LiveData<Result<AuthResponse>> login(String username, String password) {
        MutableLiveData<Result<AuthResponse>> result = new MutableLiveData<>(Result.loading());

        api.login(new LoginRequest(username, password)).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AuthResponse>> call,
                                    @NonNull Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    AuthResponse data = response.body().data;
                    tokenManager.saveTokens(data.accessToken, data.refreshToken);
                    result.postValue(Result.success(data));
                } else {
                    result.postValue(Result.error(describeHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Throwable t) {
                result.postValue(Result.error(t.getMessage() != null ? t.getMessage() : "Network error"));
            }
        });

        return result;
    }

    public LiveData<Result<MeResponse>> me() {
        MutableLiveData<Result<MeResponse>> result = new MutableLiveData<>(Result.loading());

        api.me().enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(@NonNull Call<MeResponse> call, @NonNull Response<MeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionRepository.saveLandlordId(response.body().id);
                    sessionRepository.saveUsername(response.body().username);
                    result.postValue(Result.success(response.body()));
                } else {
                    result.postValue(Result.error(describeHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MeResponse> call, @NonNull Throwable t) {
                result.postValue(Result.error(t.getMessage() != null ? t.getMessage() : "Network error"));
            }
        });

        return result;
    }

    private static String describeHttpError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                if (!body.isEmpty()) return body;
            }
        } catch (IOException ignored) {
        }
        return "HTTP " + response.code();
    }
}
