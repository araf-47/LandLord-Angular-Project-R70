package com.landlord.android.feature.settings;

import com.landlord.android.core.network.ApiResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SettingsApiService {

    @POST("api/v3/user/update")
    Call<ApiResponse<Object>> updateProfile(@Body UpdateProfileRequest request);

    @POST("api/v3/user/change-password")
    Call<ApiResponse<Object>> changePassword(@Body ChangePasswordRequest request);

    @POST("api/v3/user/toggle-2fa")
    Call<ApiResponse<Object>> toggle2fa(@Body BooleanParamRequest request);

    @POST("api/v3/user/notification-prefs")
    Call<ApiResponse<Object>> saveNotificationPrefs(@Body NotificationPrefsRequest request);

    @POST("api/v3/auth/otp")
    Call<ApiResponse<Object>> requestOtp(@Body StringParamRequest request);

    @POST("api/v3/user/logout-all")
    Call<ApiResponse<Object>> logoutAll(@Body LogoutAllRequest request);
}
