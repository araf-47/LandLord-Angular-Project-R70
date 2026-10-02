package com.landlord.android.feature.tenants;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.landlord.android.core.common.Result;
import com.landlord.android.core.network.NetworkModule;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Agreement detail is fetched live, not cached/offline - a low-frequency
 *  per-tenant detail view, same online-only treatment as Settings. Requires
 *  the tenant to already have a serverId (i.e. be synced), since the
 *  agreement itself only exists server-side as a side effect of
 *  POST /api/tenants/register. */
public class AgreementRepository {

    private final TenantApiService api;

    public AgreementRepository(Context appContext) {
        this.api = NetworkModule.getRetrofit(appContext).create(TenantApiService.class);
    }

    public LiveData<Result<RentalAgreementDto>> getAgreement(long tenantServerId) {
        MutableLiveData<Result<RentalAgreementDto>> result = new MutableLiveData<>(Result.loading());

        api.getAgreement(tenantServerId).enqueue(new Callback<RentalAgreementDto>() {
            @Override
            public void onResponse(@NonNull Call<RentalAgreementDto> call, @NonNull Response<RentalAgreementDto> response) {
                if (response.code() == 404) {
                    result.postValue(Result.error("No agreement on file yet"));
                } else if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Result.success(response.body()));
                } else {
                    result.postValue(Result.error("HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RentalAgreementDto> call, @NonNull Throwable t) {
                result.postValue(Result.error(t.getMessage() != null ? t.getMessage() : "Network error"));
            }
        });

        return result;
    }

    public LiveData<Result<RentalAgreementDto>> updateTerms(long tenantServerId, String terms) {
        MutableLiveData<Result<RentalAgreementDto>> result = new MutableLiveData<>(Result.loading());

        api.updateAgreement(tenantServerId, new UpdateAgreementRequest(terms)).enqueue(new Callback<RentalAgreementDto>() {
            @Override
            public void onResponse(@NonNull Call<RentalAgreementDto> call, @NonNull Response<RentalAgreementDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Result.success(response.body()));
                } else {
                    result.postValue(Result.error("HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RentalAgreementDto> call, @NonNull Throwable t) {
                result.postValue(Result.error(t.getMessage() != null ? t.getMessage() : "Network error"));
            }
        });

        return result;
    }
}
