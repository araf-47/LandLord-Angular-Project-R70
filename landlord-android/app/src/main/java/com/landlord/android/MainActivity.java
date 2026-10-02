package com.landlord.android;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.navigation.ui.NavigationUI;
import com.landlord.android.core.auth.BiometricLockPrefs;
import com.landlord.android.core.auth.TokenManager;
import com.landlord.android.feature.auth.AuthActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!TokenManager.getInstance(this).hasTokens()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        if (BiometricLockPrefs.isEnabled(this) && biometricAvailable()) {
            showLockScreen();
        } else {
            showMainContent();
        }
    }

    private boolean biometricAvailable() {
        BiometricManager manager = BiometricManager.from(this);
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showLockScreen() {
        setContentView(R.layout.activity_lock);
        findViewById(R.id.unlock_button).setOnClickListener(v -> promptBiometric());
        promptBiometric();
    }

    private void promptBiometric() {
        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        showMainContent();
                    }
                    // onAuthenticationFailed/onAuthenticationError leave the lock
                    // screen's "Unlock" button visible to retry - never falls
                    // through to main content on failure.
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock LandLord")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText("Cancel")
                .build();

        prompt.authenticate(info);
    }

    private void showMainContent() {
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);
    }
}
