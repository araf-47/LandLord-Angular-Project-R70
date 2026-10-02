package com.landlord.android.feature.marketplace;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class MarketplaceViewModel extends AndroidViewModel {

    private final MarketplaceRepository repository;

    public MarketplaceViewModel(@NonNull Application application) {
        super(application);
        repository = new MarketplaceRepository(application);
    }

    public LiveData<List<MarketplaceRequestEntity>> requests() {
        return repository.observeAll();
    }

    public void approve(String localId) {
        repository.decide(localId, "approved");
    }

    public void reject(String localId) {
        repository.decide(localId, "rejected");
    }
}
