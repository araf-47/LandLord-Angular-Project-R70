package com.landlord.android.feature.payments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;
import com.landlord.android.core.sync.SyncScheduler;
import com.landlord.android.core.ui.FormBottomSheet;
import com.landlord.android.feature.tenants.TenantEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentsFragment extends Fragment {

    private static final String[] PAYMENT_METHODS = {"cash", "bank", "mobile"};

    private PaymentsViewModel viewModel;
    private InvoiceAdapter invoiceAdapter;
    private BillsAdapter billsAdapter;

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

        setUpBillsSection(view);
        setUpInvoiceHistory(view);

        FloatingActionButton fab = view.findViewById(R.id.receive_payment_fab);
        fab.setOnClickListener(v -> showReceivePaymentSheet());
    }

    private void setUpBillsSection(View view) {
        RecyclerView billsList = view.findViewById(R.id.bills_list);
        TextView billsEmpty = view.findViewById(R.id.bills_empty);

        billsAdapter = new BillsAdapter();
        billsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        billsList.setAdapter(billsAdapter);

        viewModel.bills().observe(getViewLifecycleOwner(), rows -> {
            billsAdapter.submitList(rows);
            billsEmpty.setVisibility(rows == null || rows.isEmpty() ? View.VISIBLE : View.GONE);
        });

        view.findViewById(R.id.generate_bills_button).setOnClickListener(v -> {
            viewModel.generateMissingBills();
            Snackbar.make(view, "Generating bills…", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void setUpInvoiceHistory(View view) {
        RecyclerView list = view.findViewById(R.id.invoices_list);
        TextView empty = view.findViewById(R.id.invoices_empty);

        invoiceAdapter = new InvoiceAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(invoiceAdapter);

        viewModel.invoices().observe(getViewLifecycleOwner(), items -> {
            invoiceAdapter.submitList(items);
            empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showReceivePaymentSheet() {
        List<TenantEntity> tenants = viewModel.activeTenants();
        if (tenants.isEmpty()) {
            Snackbar.make(requireView(), "No active tenants to receive a payment from", Snackbar.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_receive_payment, null);

        AutoCompleteTextView tenantInput = dialogView.findViewById(R.id.input_tenant);
        TextView totalDueView = dialogView.findViewById(R.id.receive_total_due);
        AutoCompleteTextView methodInput = dialogView.findViewById(R.id.input_method);
        TextInputEditText amountInput = dialogView.findViewById(R.id.input_amount);

        List<String> tenantNames = new ArrayList<>();
        for (TenantEntity t : tenants) tenantNames.add(t.name);
        tenantInput.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, tenantNames));

        methodInput.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, PAYMENT_METHODS));
        methodInput.setText(PAYMENT_METHODS[0], false);

        TenantEntity[] selectedTenant = new TenantEntity[1];
        tenantInput.setOnItemClickListener((parent, v, position, id) -> {
            String name = (String) parent.getItemAtPosition(position);
            for (TenantEntity t : tenants) {
                if (t.name != null && t.name.equals(name)) {
                    selectedTenant[0] = t;
                    break;
                }
            }
            if (selectedTenant[0] != null) {
                double due = viewModel.totalDueForTenant(selectedTenant[0].localId);
                totalDueView.setText(String.format(Locale.getDefault(), "Total due: %.2f", due));
                totalDueView.setVisibility(View.VISIBLE);
            }
        });

        FormBottomSheet.show(requireContext(), "Receive payment", "Save payment", dialogView, () -> {
            if (selectedTenant[0] == null) {
                Snackbar.make(requireView(), "Choose a tenant first", Snackbar.LENGTH_LONG).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(String.valueOf(amountInput.getText()).trim());
            } catch (NumberFormatException e) {
                Snackbar.make(requireView(), "Enter an amount", Snackbar.LENGTH_LONG).show();
                return;
            }
            if (amount <= 0) {
                Snackbar.make(requireView(), "Enter an amount", Snackbar.LENGTH_LONG).show();
                return;
            }

            String method = methodInput.getText().toString().trim();
            if (method.isEmpty()) method = PAYMENT_METHODS[0];

            String tenantLocalId = selectedTenant[0].localId;
            double totalDueBefore = viewModel.totalDueForTenant(tenantLocalId);

            double remaining = amount;
            for (InvoiceEntity invoice : viewModel.oldestUnpaidInvoicesForTenant(tenantLocalId)) {
                if (remaining <= 0) break;
                double balance = invoice.balance != null ? invoice.balance : 0;
                double applied = Math.min(remaining, balance);
                if (applied <= 0) continue;
                viewModel.recordPayment(invoice.localId, tenantLocalId, applied, method);
                remaining -= applied;
            }

            String message = amount >= totalDueBefore
                    ? "Payment saved. Marked fully paid."
                    : String.format(Locale.getDefault(),
                            "Payment saved. Marked partially paid — remaining %.2f.", totalDueBefore - amount);
            Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
        });
    }
}
