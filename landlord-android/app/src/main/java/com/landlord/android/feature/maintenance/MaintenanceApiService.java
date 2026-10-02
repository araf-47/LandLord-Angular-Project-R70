package com.landlord.android.feature.maintenance;

import java.util.List;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface MaintenanceApiService {

    @GET("api/maintenance-tickets")
    Call<List<MaintenanceTicketDto>> list();

    @POST("api/maintenance-tickets")
    Call<MaintenanceTicketDto> create(@Body NewTicketRequest request);

    @PUT("api/maintenance-tickets/{id}/status")
    Call<MaintenanceTicketDto> updateStatus(@Path("id") long id, @Body UpdateTicketStatusRequest request);

    @Multipart
    @POST("api/maintenance-tickets/{id}/photo")
    Call<MaintenanceTicketDto> uploadPhoto(@Path("id") long id, @Part MultipartBody.Part file);
}
