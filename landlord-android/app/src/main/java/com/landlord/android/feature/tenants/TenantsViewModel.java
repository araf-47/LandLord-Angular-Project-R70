package com.landlord.android.feature.tenants;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.landlord.android.feature.properties.UnitEntity;
import java.util.List;

public class TenantsViewModel extends AndroidViewModel {

    private final TenantRepository repository;

    public TenantsViewModel(@NonNull Application application) {
        super(application);
        repository = new TenantRepository(application);
    }

    public LiveData<List<TenantEntity>> tenants() {
        return repository.observeAll();
    }

    public LiveData<List<UnitEntity>> units() {
        return repository.units();
    }

    public void registerTenant(String unitLocalId, String name, String phone,
                                String email, String nationalId, String terms, double deposit) {
        repository.registerTenant(unitLocalId, name, phone, email, nationalId, terms, deposit);
    }
}
