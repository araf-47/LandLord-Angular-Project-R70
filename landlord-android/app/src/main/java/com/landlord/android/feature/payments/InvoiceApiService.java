package com.landlord.android.feature.payments;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface InvoiceApiService {

    @GET("api/invoices")
    Call<List<InvoiceDto>> list();

    @POST("api/invoices/generate")
    Call<InvoiceDto> generate(@Body InvoiceGenerateRequest request);
}
