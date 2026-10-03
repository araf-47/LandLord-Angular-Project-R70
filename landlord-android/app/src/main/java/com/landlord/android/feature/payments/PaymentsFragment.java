package com.landlord.android.feature.payments;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;
import com.landlord.android.core.ui.FormBottomSheet;
import com.landlord.android.feature.tenants.TenantEntity;
import java.util.ArrayList;
import java.util.List;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.landlord.android.core.sync.SyncScheduler;

public class PaymentsFragment extends Fragment {

    private PaymentsViewModel viewModel;
    private InvoiceAdapter adapter;
    private List<TenantEntity> availableTenants = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_payments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.invoices_list_refresh);
        refreshLayout.setColorSchemeResources(R.color.md_primary);
        refreshLayout.setOnRefreshListener(() -> {
            SyncScheduler.requestImmediateSync(requireContext().getApplicationContext());
            view.postDelayed(() -> refreshLayout.setRefreshing(false), 800);
        });

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(PaymentsViewModel.class);

        RecyclerView list = view.findViewById(R.id.invoices_list);
        TextView empty = view.findViewById(R.id.invoices_empty);
        FloatingActionButton fab = view.findViewById(R.id.add_invoice_fab);

        adapter = new InvoiceAdapter(this::showRecordPaymentDialog);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.invoices().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.tenants().observe(getViewLifecycleOwner(), tenants -> availableTenants = tenants);

        fab.setOnClickListener(v -> showGenerateInvoiceDialog());
    }

    private void showGenerateInvoiceDialog() {
        if (availableTenants.isEmpty()) {
            Snackbar.make(requireView(), "Register a tenant first", Snackbar.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_generate_invoice, null);

        Spinner tenantSpinner = dialogView.findViewById(R.id.input_tenant);
        TextInputEditText utilitiesInput = dialogView.findViewById(R.id.input_utilities);

        List<String> labels = new ArrayList<>();
        for (TenantEntity t : availableTenants) labels.add(t.name);

        tenantSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, labels));

        FormBottomSheet.show(requireContext(), "Generate invoice", "Generate", dialogView, () -> {
            int selected = tenantSpinner.getSelectedItemPosition();
            if (selected < 0 || selected >= availableTenants.size()) return;

            double utilities;
            try {
                utilities = Double.parseDouble(String.valueOf(utilitiesInput.getText()).trim());
            } catch (NumberFormatException e) {
                utilities = 0;
            }

            viewModel.generateInvoice(availableTenants.get(selected).localId, utilities);
        });
    }

    private void showRecordPaymentDialog(InvoiceEntity invoice) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_record_payment, null);

        TextInputEditText amountInput = dialogView.findViewById(R.id.input_amount);
        TextInputEditText methodInput = dialogView.findViewById(R.id.input_method);

        FormBottomSheet.show(requireContext(), "Record payment", "Save", dialogView, () -> {
            double amount;
            try {
                amount = Double.parseDouble(String.valueOf(amountInput.getText()).trim());
            } catch (NumberFormatException e) {
                return;
            }

            String method = String.valueOf(methodInput.getText()).trim();
            if (method.isEmpty()) method = "cash";

            viewModel.recordPayment(invoice.localId, invoice.tenantLocalId, amount, method);
        });
    }
}
