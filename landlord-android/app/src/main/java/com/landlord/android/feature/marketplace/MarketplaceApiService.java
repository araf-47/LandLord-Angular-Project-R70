package com.landlord.android.feature.marketplace;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface MarketplaceApiService {

    @GET("api/marketplace-requests")
    Call<List<MarketplaceRequestDto>> list();

    @PUT("api/marketplace-requests/{id}/status")
    Call<MarketplaceRequestDto> updateStatus(@Path("id") long id, @Body DecisionRequest request);
}
