package com.landlord.android.feature.properties;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.landlord.android.R;

public class MoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.more_marketplace).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_marketplace));

        view.findViewById(R.id.more_expenses).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_expenses));

        view.findViewById(R.id.more_ledger).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_ledger));

        view.findViewById(R.id.more_payment_receipts).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_payment_history));

        view.findViewById(R.id.more_maintenance).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_maintenance));

        view.findViewById(R.id.more_messages).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_messages));

        view.findViewById(R.id.more_notifications).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_notifications));

        view.findViewById(R.id.more_settings).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_settings));

        view.findViewById(R.id.more_reports).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_reports));

        view.findViewById(R.id.more_insights).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_insights));
    }
}
