package com.landlord.android.feature.auth;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.landlord.android.core.auth.AuthRepository;
import com.landlord.android.core.auth.AuthResponse;
import com.landlord.android.core.common.Result;

public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository repository;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public LiveData<Result<AuthResponse>> login(String username, String password) {
        return repository.login(username, password);
    }
}
