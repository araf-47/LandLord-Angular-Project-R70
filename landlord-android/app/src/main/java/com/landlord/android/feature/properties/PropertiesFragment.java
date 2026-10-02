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
import androidx.navigation.Navigation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;

public class PropertiesFragment extends Fragment {

    private PropertiesViewModel viewModel;
    private PropertyAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_properties, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(PropertiesViewModel.class);

        RecyclerView list = view.findViewById(R.id.properties_list);
        TextView empty = view.findViewById(R.id.properties_empty);
        FloatingActionButton fab = view.findViewById(R.id.add_property_fab);

        adapter = new PropertyAdapter(entity -> {
            Bundle args = new Bundle();
            args.putString("propertyLocalId", entity.localId);
            Navigation.findNavController(view).navigate(R.id.action_properties_to_detail, args);
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.properties().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        fab.setOnClickListener(v -> showAddDialog());
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_property, null);

        TextInputEditText name = dialogView.findViewById(R.id.input_name);
        TextInputEditText address = dialogView.findViewById(R.id.input_address);
        TextInputEditText district = dialogView.findViewById(R.id.input_district);
        TextInputEditText area = dialogView.findViewById(R.id.input_area);
        TextInputEditText type = dialogView.findViewById(R.id.input_type);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add property")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String nameValue = String.valueOf(name.getText()).trim();
                    String addressValue = String.valueOf(address.getText()).trim();
                    if (nameValue.isEmpty() || addressValue.isEmpty()) return;

                    viewModel.createProperty(
                            nameValue,
                            addressValue,
                            String.valueOf(district.getText()).trim(),
                            String.valueOf(area.getText()).trim(),
                            String.valueOf(type.getText()).trim()
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
