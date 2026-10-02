package com.landlord.android.feature.tenants;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface TenantApiService {

    @GET("api/tenants")
    Call<List<TenantDto>> list();

    @POST("api/tenants/register")
    Call<TenantDto> register(@Body TenantRegisterRequest request);

    @GET("api/tenants/{tenantId}/agreement")
    Call<RentalAgreementDto> getAgreement(@Path("tenantId") long tenantId);

    @PUT("api/tenants/{tenantId}/agreement")
    Call<RentalAgreementDto> updateAgreement(@Path("tenantId") long tenantId, @Body UpdateAgreementRequest request);
}
