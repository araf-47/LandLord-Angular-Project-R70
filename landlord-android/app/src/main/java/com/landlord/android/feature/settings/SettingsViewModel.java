package com.landlord.android.feature.settings;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.landlord.android.core.auth.AuthRepository;
import com.landlord.android.core.auth.MeResponse;
import com.landlord.android.core.common.Result;

public class SettingsViewModel extends AndroidViewModel {

    private final SettingsRepository settingsRepository;
    private final AuthRepository authRepository;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        settingsRepository = new SettingsRepository(application);
        authRepository = new AuthRepository(application);
    }

    public LiveData<Result<MeResponse>> me() {
        return authRepository.me();
    }

    public LiveData<Result<Void>> updateProfile(String email, String phone) {
        return settingsRepository.updateProfile(email, phone);
    }

    public LiveData<Result<Void>> requestOtp() {
        return settingsRepository.requestOtp();
    }

    public LiveData<Result<Void>> changePassword(String oldPassword, String newPassword, String otp) {
        return settingsRepository.changePassword(oldPassword, newPassword, otp);
    }

    public LiveData<Result<Void>> toggle2fa(boolean enabled) {
        return settingsRepository.toggle2fa(enabled);
    }

    public LiveData<Result<Void>> saveNotificationPrefs(boolean rentDueEmail, boolean rentDueSms,
                                                         boolean paymentReceivedEmail, boolean maintenanceEmail) {
        return settingsRepository.saveNotificationPrefs(rentDueEmail, rentDueSms, paymentReceivedEmail, maintenanceEmail);
    }
}
