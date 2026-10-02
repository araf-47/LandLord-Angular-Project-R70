package com.landlord.android.feature.expenses;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ExpenseApiService {

    @GET("api/expenses")
    Call<List<ExpenseDto>> list();

    @POST("api/expenses")
    Call<ExpenseDto> create(@Body ExpenseCreateRequest request);
}
