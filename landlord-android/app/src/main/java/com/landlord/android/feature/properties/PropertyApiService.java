package com.landlord.android.feature.properties;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface PropertyApiService {

    @GET("api/properties")
    Call<List<PropertyDto>> list();

    @POST("api/properties")
    Call<PropertyDto> create(@Body PropertyDto property);
}
