package com.landlord.android.feature.properties;

import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;
import com.landlord.android.core.ui.FormBottomSheet;

public class PropertyDetailFragment extends Fragment {

    private PropertyDetailViewModel viewModel;
    private UnitAdapter adapter;
    private String propertyLocalId;
    private boolean enterTransitionStarted = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setSharedElementEnterTransition(TransitionInflater.from(requireContext())
                .inflateTransition(android.R.transition.move));
        postponeEnterTransition();
        return inflater.inflate(R.layout.fragment_property_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        propertyLocalId = requireArguments().getString("propertyLocalId");

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(PropertyDetailViewModel.class);

        View headerCard = view.findViewById(R.id.detail_header_card);
        headerCard.setTransitionName("property_card_" + propertyLocalId);

        TextView nameView = view.findViewById(R.id.detail_property_name);
        RecyclerView list = view.findViewById(R.id.units_list);
        FloatingActionButton fab = view.findViewById(R.id.add_unit_fab);

        adapter = new UnitAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        view.postDelayed(this::startEnterTransitionOnce, 300);

        viewModel.property(propertyLocalId).observe(getViewLifecycleOwner(), property -> {
            if (property != null) nameView.setText(property.name);
            startEnterTransitionOnce();
        });

        viewModel.units(propertyLocalId).observe(getViewLifecycleOwner(), adapter::submitList);

        fab.setOnClickListener(v -> showAddUnitDialog());
    }

    private void startEnterTransitionOnce() {
        if (enterTransitionStarted) return;
        enterTransitionStarted = true;
        startPostponedEnterTransition();
    }

    private void showAddUnitDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_unit, null);

        TextInputEditText numberInput = dialogView.findViewById(R.id.input_unit_number);
        TextInputEditText rentInput = dialogView.findViewById(R.id.input_rent);

        FormBottomSheet.show(requireContext(), "Add unit", "Save", dialogView, () -> {
            String number = String.valueOf(numberInput.getText()).trim();
            if (number.isEmpty()) return;

            double rent;
            try {
                rent = Double.parseDouble(String.valueOf(rentInput.getText()).trim());
            } catch (NumberFormatException e) {
                rent = 0;
            }

            viewModel.addUnit(propertyLocalId, number, rent);
        });
    }
}
