package com.landlord.android.feature.properties;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface UnitApiService {

    @GET("api/units")
    Call<List<UnitDto>> list();

    @POST("api/units")
    Call<UnitDto> create(@Body UnitDto unit);
}
