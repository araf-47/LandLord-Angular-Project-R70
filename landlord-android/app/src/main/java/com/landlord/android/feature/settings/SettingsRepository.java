package com.landlord.android.feature.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.landlord.android.core.auth.SessionRepository;
import com.landlord.android.core.common.Result;
import com.landlord.android.core.network.ApiResponse;
import com.landlord.android.core.network.NetworkModule;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Account/security actions are inherently online-only - a password change
 *  needs a fresh OTP and a profile update should fail loudly if offline,
 *  not queue silently. No outbox here, unlike every other feature module. */
public class SettingsRepository {

    private final SettingsApiService api;
    private final SessionRepository sessionRepository;

    public SettingsRepository(Context appContext) {
        this.api = NetworkModule.settingsApi(appContext);
        this.sessionRepository = SessionRepository.getInstance(appContext);
    }

    public LiveData<Result<Void>> updateProfile(String email, String phone) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.id = sessionRepository.getLandlordId();
        request.email = email;
        request.phone = phone;
        return call(api.updateProfile(request));
    }

    public LiveData<Result<Void>> requestOtp() {
        return call(api.requestOtp(new StringParamRequest(sessionRepository.getUsername())));
    }

    public LiveData<Result<Void>> changePassword(String oldPassword, String newPassword, String otp) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.oldPassword = oldPassword;
        request.password = newPassword;
        request.otp = otp;
        return call(api.changePassword(request));
    }

    public LiveData<Result<Void>> toggle2fa(boolean enabled) {
        return call(api.toggle2fa(new BooleanParamRequest(enabled)));
    }

    public LiveData<Result<Void>> saveNotificationPrefs(boolean rentDueEmail, boolean rentDueSms,
                                                         boolean paymentReceivedEmail, boolean maintenanceEmail) {
        NotificationPrefsRequest request = new NotificationPrefsRequest();
        request.notifyRentDueEmail = rentDueEmail;
        request.notifyRentDueSms = rentDueSms;
        request.notifyPaymentReceivedEmail = paymentReceivedEmail;
        request.notifyMaintenanceEmail = maintenanceEmail;
        return call(api.saveNotificationPrefs(request));
    }

    private LiveData<Result<Void>> call(Call<ApiResponse<Object>> call) {
        MutableLiveData<Result<Void>> result = new MutableLiveData<>(Result.loading());

        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    result.postValue(Result.success(null));
                } else {
                    result.postValue(Result.error(describeHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
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
