package com.landlord.android.feature.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;
import com.landlord.android.core.common.Result;

public class SettingsFragment extends Fragment {

    private static final String APPEARANCE_PREFS = "appearance_prefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private SettingsViewModel viewModel;
    private boolean suppressToggleCallbacks = true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(SettingsViewModel.class);

        TextInputEditText serverUrlInput = view.findViewById(R.id.input_server_url);
        serverUrlInput.setText(com.landlord.android.core.network.ServerConfig.getBaseUrl(requireContext()));
        view.findViewById(R.id.save_server_button).setOnClickListener(v -> {
            String url = String.valueOf(serverUrlInput.getText()).trim();
            if (url.isEmpty()) return;
            com.landlord.android.core.network.ServerConfig.setBaseUrl(requireContext(), url);
            Snackbar.make(view, "Server address saved", Snackbar.LENGTH_SHORT).show();
        });

        TextInputEditText emailInput = view.findViewById(R.id.input_email);
        TextInputEditText phoneInput = view.findViewById(R.id.input_phone);
        MaterialSwitch switch2fa = view.findViewById(R.id.switch_2fa);
        MaterialSwitch switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        MaterialSwitch switchRentEmail = view.findViewById(R.id.switch_rent_due_email);
        MaterialSwitch switchRentSms = view.findViewById(R.id.switch_rent_due_sms);
        MaterialSwitch switchPaymentEmail = view.findViewById(R.id.switch_payment_received_email);
        MaterialSwitch switchMaintenanceEmail = view.findViewById(R.id.switch_maintenance_email);
        android.widget.ProgressBar progress = view.findViewById(R.id.settings_progress);

        View biometricRow = view.findViewById(R.id.biometric_lock_row);
        MaterialSwitch switchBiometricLock = view.findViewById(R.id.switch_biometric_lock);
        boolean biometricSupported = BiometricManager.from(requireContext())
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
        if (biometricSupported) {
            biometricRow.setVisibility(View.VISIBLE);
            switchBiometricLock.setChecked(com.landlord.android.core.auth.BiometricLockPrefs.isEnabled(requireContext()));
            switchBiometricLock.setOnCheckedChangeListener((btn, checked) ->
                    com.landlord.android.core.auth.BiometricLockPrefs.setEnabled(requireContext(), checked));
        }

        SharedPreferences appearancePrefs = requireContext().getSharedPreferences(APPEARANCE_PREFS, 0);
        switchDarkMode.setChecked(appearancePrefs.getBoolean(KEY_DARK_MODE, false));
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            appearancePrefs.edit().putBoolean(KEY_DARK_MODE, checked).apply();
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        viewModel.me().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                com.landlord.android.core.auth.MeResponse me = ((Result.Success<com.landlord.android.core.auth.MeResponse>) result).data;
                emailInput.setText(me.email);
                phoneInput.setText(me.phone);
                switch2fa.setChecked(me.twoFactorEnabled);
                switchRentEmail.setChecked(me.notifyRentDueEmail);
                switchRentSms.setChecked(me.notifyRentDueSms);
                switchPaymentEmail.setChecked(me.notifyPaymentReceivedEmail);
                switchMaintenanceEmail.setChecked(me.notifyMaintenanceEmail);
                suppressToggleCallbacks = false;
            } else if (result instanceof Result.Error) {
                Snackbar.make(view, "Couldn't load settings: " + ((Result.Error<?>) result).message, Snackbar.LENGTH_LONG).show();
            }
        });

        switch2fa.setOnCheckedChangeListener((btn, checked) -> {
            if (suppressToggleCallbacks) return;
            viewModel.toggle2fa(checked).observe(getViewLifecycleOwner(), result ->
                    showResultSnackbar(view, result, "2FA updated"));
        });

        view.findViewById(R.id.save_profile_button).setOnClickListener(v ->
                viewModel.updateProfile(
                        String.valueOf(emailInput.getText()).trim(),
                        String.valueOf(phoneInput.getText()).trim()
                ).observe(getViewLifecycleOwner(), result -> showResultSnackbar(view, result, "Profile saved")));

        view.findViewById(R.id.save_prefs_button).setOnClickListener(v ->
                viewModel.saveNotificationPrefs(
                        switchRentEmail.isChecked(),
                        switchRentSms.isChecked(),
                        switchPaymentEmail.isChecked(),
                        switchMaintenanceEmail.isChecked()
                ).observe(getViewLifecycleOwner(), result -> showResultSnackbar(view, result, "Preferences saved")));

        view.findViewById(R.id.change_password_button).setOnClickListener(v -> showChangePasswordDialog(view));

        view.findViewById(R.id.logout_button).setOnClickListener(v -> confirmLogout());
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out")
                .setMessage("This clears all locally saved data on this device. Any changes not yet synced will be lost. Continue?")
                .setPositiveButton("Log out", (dialog, which) -> doLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doLogout() {
        com.landlord.android.core.auth.LogoutHelper.logout(requireContext());

        Intent intent = new Intent(requireContext(), com.landlord.android.feature.auth.AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showChangePasswordDialog(View rootView) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        TextInputEditText oldPassword = dialogView.findViewById(R.id.input_old_password);
        TextInputEditText newPassword = dialogView.findViewById(R.id.input_new_password);
        TextInputEditText otp = dialogView.findViewById(R.id.input_otp);

        dialogView.findViewById(R.id.request_otp_button).setOnClickListener(v ->
                viewModel.requestOtp().observe(getViewLifecycleOwner(), result ->
                        showResultSnackbar(rootView, result, "OTP sent")));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change password")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) ->
                        viewModel.changePassword(
                                String.valueOf(oldPassword.getText()),
                                String.valueOf(newPassword.getText()),
                                String.valueOf(otp.getText()).trim()
                        ).observe(getViewLifecycleOwner(), result -> showResultSnackbar(rootView, result, "Password changed")))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResultSnackbar(View view, Result<?> result, String successMessage) {
        if (result instanceof Result.Success) {
            Snackbar.make(view, successMessage, Snackbar.LENGTH_SHORT).show();
        } else if (result instanceof Result.Error) {
            Snackbar.make(view, "Failed: " + ((Result.Error<?>) result).message, Snackbar.LENGTH_LONG).show();
        }
    }
}
