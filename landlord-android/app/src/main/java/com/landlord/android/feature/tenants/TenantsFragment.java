package com.landlord.android.feature.tenants;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;
import com.landlord.android.feature.properties.UnitEntity;
import java.util.ArrayList;
import java.util.List;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.landlord.android.core.sync.SyncScheduler;

public class TenantsFragment extends Fragment {

    private TenantsViewModel viewModel;
    private TenantAdapter adapter;
    private List<UnitEntity> availableUnits = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tenants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.tenants_list_refresh);
        refreshLayout.setColorSchemeResources(R.color.md_primary);
        refreshLayout.setOnRefreshListener(() -> {
            SyncScheduler.requestImmediateSync(requireContext().getApplicationContext());
            view.postDelayed(() -> refreshLayout.setRefreshing(false), 800);
        });

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(TenantsViewModel.class);

        RecyclerView list = view.findViewById(R.id.tenants_list);
        TextView empty = view.findViewById(R.id.tenants_empty);
        FloatingActionButton fab = view.findViewById(R.id.add_tenant_fab);

        adapter = new TenantAdapter(entity -> {
            Bundle args = new Bundle();
            args.putString("tenantLocalId", entity.localId);
            Navigation.findNavController(view).navigate(R.id.action_tenants_to_detail, args);
        });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.tenants().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.units().observe(getViewLifecycleOwner(), units -> availableUnits = units);

        fab.setOnClickListener(v -> showRegisterDialog());
    }

    private void showRegisterDialog() {
        if (availableUnits.isEmpty()) {
            com.google.android.material.snackbar.Snackbar.make(requireView(),
                    "Add a property and unit first", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_register_tenant, null);

        Spinner unitSpinner = dialogView.findViewById(R.id.input_unit);
        TextInputEditText nameInput = dialogView.findViewById(R.id.input_name);
        TextInputEditText phoneInput = dialogView.findViewById(R.id.input_phone);
        TextInputEditText emailInput = dialogView.findViewById(R.id.input_email);
        TextInputEditText nidInput = dialogView.findViewById(R.id.input_nid);
        TextInputEditText depositInput = dialogView.findViewById(R.id.input_deposit);

        List<String> unitLabels = new ArrayList<>();
        for (UnitEntity u : availableUnits) unitLabels.add(u.unitNumber + " (" + u.status + ")");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, unitLabels);
        unitSpinner.setAdapter(spinnerAdapter);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Register tenant")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    int selected = unitSpinner.getSelectedItemPosition();
                    if (selected < 0 || selected >= availableUnits.size()) return;

                    String name = String.valueOf(nameInput.getText()).trim();
                    String nationalId = String.valueOf(nidInput.getText()).trim();
                    if (name.isEmpty() || nationalId.isEmpty()) return;

                    double deposit;
                    try {
                        deposit = Double.parseDouble(String.valueOf(depositInput.getText()).trim());
                    } catch (NumberFormatException e) {
                        deposit = 0;
                    }

                    viewModel.registerTenant(
                            availableUnits.get(selected).localId,
                            name,
                            String.valueOf(phoneInput.getText()).trim(),
                            String.valueOf(emailInput.getText()).trim(),
                            nationalId,
                            "Standard lease",
                            deposit
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
