package com.landlord.android.feature.dashboard;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.landlord.android.core.auth.AuthRepository;
import com.landlord.android.core.auth.MeResponse;
import com.landlord.android.core.common.Result;

public class DashboardViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private LiveData<Result<MeResponse>> meResult;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public LiveData<Result<MeResponse>> me() {
        if (meResult == null) {
            meResult = repository.me();
        }
        return meResult;
    }
}
