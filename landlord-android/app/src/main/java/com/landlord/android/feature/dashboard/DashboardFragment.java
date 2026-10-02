package com.landlord.android.feature.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.landlord.android.R;
import com.landlord.android.core.common.Result;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(DashboardViewModel.class);

        ProgressBar progress = view.findViewById(R.id.dashboard_progress);
        TextView status = view.findViewById(R.id.dashboard_status);

        viewModel.me().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                progress.setVisibility(View.VISIBLE);
                status.setText("");
            } else if (result instanceof Result.Success) {
                progress.setVisibility(View.GONE);
                Object data = ((Result.Success<?>) result).data;
                status.setText(describe(data));
            } else if (result instanceof Result.Error) {
                progress.setVisibility(View.GONE);
                status.setText("Couldn't load profile: " + ((Result.Error<?>) result).message);
            }
        });
    }

    private String describe(Object meResponseObj) {
        com.landlord.android.core.auth.MeResponse me = (com.landlord.android.core.auth.MeResponse) meResponseObj;
        StringBuilder sb = new StringBuilder();
        sb.append("Logged in as ").append(me.username);
        if (me.roles != null && !me.roles.isEmpty()) {
            sb.append(" (").append(String.join(", ", me.roles)).append(")");
        }
        if (me.email != null) sb.append("\nEmail: ").append(me.email);
        if (me.phone != null) sb.append("\nPhone: ").append(me.phone);
        return sb.toString();
    }
}
