package com.landlord.android.feature.insights;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface InsightsApiService {

    @POST("api/insights/chat")
    Call<ChatResponse> chat(@Body ChatRequest request);
}
