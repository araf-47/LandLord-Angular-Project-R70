package com.landlord.android.core.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.landlord.android.R;
import android.widget.TextView;

public abstract class PlaceholderFragment extends Fragment {

    protected abstract String title();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        TextView titleView = view.findViewById(R.id.placeholder_title);
        titleView.setText(title());
        return view;
    }
}
