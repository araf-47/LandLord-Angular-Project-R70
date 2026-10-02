package com.landlord.android.feature.notifications;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NotificationApiService {

    @GET("api/notifications")
    Call<List<NotificationDto>> list(@Query("audience") String audience);

    @PUT("api/notifications/{id}/read")
    Call<Void> markRead(@Path("id") long id);
}
