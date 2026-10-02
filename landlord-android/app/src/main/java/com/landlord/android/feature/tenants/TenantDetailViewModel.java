package com.landlord.android.feature.tenants;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.landlord.android.core.common.Result;
import com.landlord.android.core.db.AppDatabase;

public class TenantDetailViewModel extends AndroidViewModel {

    private final AgreementRepository agreementRepository;
    private final TenantDao tenantDao;

    public TenantDetailViewModel(@NonNull Application application) {
        super(application);
        agreementRepository = new AgreementRepository(application);
        tenantDao = AppDatabase.getInstance(application).tenantDao();
    }

    public LiveData<TenantEntity> tenant(String tenantLocalId) {
        return tenantDao.observeByLocalId(tenantLocalId);
    }

    public LiveData<Result<RentalAgreementDto>> getAgreement(long tenantServerId) {
        return agreementRepository.getAgreement(tenantServerId);
    }

    public LiveData<Result<RentalAgreementDto>> updateTerms(long tenantServerId, String terms) {
        return agreementRepository.updateTerms(tenantServerId, terms);
    }
}
