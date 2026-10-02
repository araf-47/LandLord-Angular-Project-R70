package com.landlord.android.feature.properties;

import android.os.Bundle;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;

public class PropertyDetailFragment extends Fragment {

    private PropertyDetailViewModel viewModel;
    private UnitAdapter adapter;
    private String propertyLocalId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_property_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        propertyLocalId = requireArguments().getString("propertyLocalId");

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(PropertyDetailViewModel.class);

        TextView nameView = view.findViewById(R.id.detail_property_name);
        RecyclerView list = view.findViewById(R.id.units_list);
        FloatingActionButton fab = view.findViewById(R.id.add_unit_fab);

        adapter = new UnitAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.property(propertyLocalId).observe(getViewLifecycleOwner(), property -> {
            if (property != null) nameView.setText(property.name);
        });

        viewModel.units(propertyLocalId).observe(getViewLifecycleOwner(), adapter::submitList);

        fab.setOnClickListener(v -> showAddUnitDialog());
    }

    private void showAddUnitDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_unit, null);

        TextInputEditText numberInput = dialogView.findViewById(R.id.input_unit_number);
        TextInputEditText rentInput = dialogView.findViewById(R.id.input_rent);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add unit")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String number = String.valueOf(numberInput.getText()).trim();
                    if (number.isEmpty()) return;

                    double rent;
                    try {
                        rent = Double.parseDouble(String.valueOf(rentInput.getText()).trim());
                    } catch (NumberFormatException e) {
                        rent = 0;
                    }

                    viewModel.addUnit(propertyLocalId, number, rent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
