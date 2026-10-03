package com.landlord.android.feature.properties;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import com.landlord.android.R;

public class MoreFragment extends Fragment {

    private static final NavOptions FADE_NAV_OPTIONS = new NavOptions.Builder()
            .setEnterAnim(R.anim.nav_fade_in)
            .setExitAnim(R.anim.nav_fade_out)
            .setPopEnterAnim(R.anim.nav_fade_in)
            .setPopExitAnim(R.anim.nav_fade_out)
            .build();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.more_marketplace).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_marketplace, null, FADE_NAV_OPTIONS));

        view.findViewById(R.id.more_expenses).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_expenses, null, FADE_NAV_OPTIONS));

        view.findViewById(R.id.more_ledger).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_ledger, null, FADE_NAV_OPTIONS));

        view.findViewById(R.id.more_payment_receipts).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_payment_history, null, FADE_NAV_OPTIONS));

        view.findViewById(R.id.more_maintenance).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_maintenance, null, FADE_NAV_OPTIONS));

        view.findViewById(R.id.more_messages).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_messages, null, FADE_NAV_OPTIONS));

        view.findViewById(R.id.more_notifications).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_notifications, null, FADE_NAV_OPTIONS));

        view.findViewById(R.id.more_settings).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_settings, null, FADE_NAV_OPTIONS));

        view.findViewById(R.id.more_reports).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_reports, null, FADE_NAV_OPTIONS));

        view.findViewById(R.id.more_insights).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.nav_insights, null, FADE_NAV_OPTIONS));
    }
}
