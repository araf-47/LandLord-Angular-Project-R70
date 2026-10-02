package com.landlord.android.feature.payments;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

public interface PaymentApiService {

    @GET("api/payments")
    Call<List<PaymentDto>> list();

    @POST("api/payments")
    Call<PaymentDto> create(@Body RecordPaymentRequest request);

    @Streaming
    @GET("api/payments/{id}/receipt")
    Call<ResponseBody> receipt(@Path("id") long paymentId);
}
