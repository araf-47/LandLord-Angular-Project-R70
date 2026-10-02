package com.landlord.android.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.MainActivity;
import com.landlord.android.R;
import com.landlord.android.core.common.Result;
import com.landlord.android.core.sync.SyncScheduler;

public class AuthActivity extends AppCompatActivity {

    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        TextInputEditText serverUrlInput = findViewById(R.id.server_url_input);
        TextInputEditText usernameInput = findViewById(R.id.username_input);
        TextInputEditText passwordInput = findViewById(R.id.password_input);
        MaterialButton loginButton = findViewById(R.id.login_button);
        ProgressBar progress = findViewById(R.id.login_progress);
        TextView errorText = findViewById(R.id.error_text);

        serverUrlInput.setText(com.landlord.android.core.network.ServerConfig.getBaseUrl(this));

        loginButton.setOnClickListener(v -> {
            String serverUrl = String.valueOf(serverUrlInput.getText()).trim();
            String username = String.valueOf(usernameInput.getText()).trim();
            String password = String.valueOf(passwordInput.getText());

            if (serverUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
                errorText.setText("Enter server address, username and password");
                errorText.setVisibility(View.VISIBLE);
                return;
            }

            com.landlord.android.core.network.ServerConfig.setBaseUrl(this, serverUrl);
            errorText.setVisibility(View.GONE);

            viewModel.login(username, password).observe(this, result -> {
                if (result instanceof Result.Loading) {
                    progress.setVisibility(View.VISIBLE);
                    loginButton.setEnabled(false);
                } else if (result instanceof Result.Success) {
                    progress.setVisibility(View.GONE);
                    SyncScheduler.ensurePeriodicSyncScheduled(getApplicationContext());
                    SyncScheduler.requestImmediateSync(getApplicationContext());
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else if (result instanceof Result.Error) {
                    progress.setVisibility(View.GONE);
                    loginButton.setEnabled(true);
                    errorText.setText(((Result.Error<?>) result).message);
                    errorText.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}
