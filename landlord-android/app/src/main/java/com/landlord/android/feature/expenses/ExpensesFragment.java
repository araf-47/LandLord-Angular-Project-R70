package com.landlord.android.feature.expenses;

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
import com.landlord.android.feature.properties.PropertyEntity;
import java.util.ArrayList;
import java.util.List;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.landlord.android.core.sync.SyncScheduler;

public class ExpensesFragment extends Fragment {

    private ExpensesViewModel viewModel;
    private ExpenseAdapter adapter;
    private List<PropertyEntity> availableProperties = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expenses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.expenses_list_refresh);
        refreshLayout.setColorSchemeResources(R.color.md_primary);
        refreshLayout.setOnRefreshListener(() -> {
            SyncScheduler.requestImmediateSync(requireContext().getApplicationContext());
            view.postDelayed(() -> refreshLayout.setRefreshing(false), 800);
        });

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(ExpensesViewModel.class);

        RecyclerView list = view.findViewById(R.id.expenses_list);
        TextView empty = view.findViewById(R.id.expenses_empty);
        FloatingActionButton fab = view.findViewById(R.id.add_expense_fab);

        adapter = new ExpenseAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        viewModel.expenses().observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            empty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.properties().observe(getViewLifecycleOwner(), props -> availableProperties = props);

        fab.setOnClickListener(v -> showAddDialog());
    }

    private void showAddDialog() {
        if (availableProperties.isEmpty()) {
            Snackbar.make(requireView(), "Add a property first", Snackbar.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_expense, null);

        Spinner propertySpinner = dialogView.findViewById(R.id.input_property);
        TextInputEditText categoryInput = dialogView.findViewById(R.id.input_category);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.input_description);
        TextInputEditText amountInput = dialogView.findViewById(R.id.input_amount);

        List<String> labels = new ArrayList<>();
        for (PropertyEntity p : availableProperties) labels.add(p.name);
        propertySpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, labels));

        FormBottomSheet.show(requireContext(), "Add expense", "Save", dialogView, () -> {
            int selected = propertySpinner.getSelectedItemPosition();
            if (selected < 0 || selected >= availableProperties.size()) return;

            String category = String.valueOf(categoryInput.getText()).trim();
            if (category.isEmpty()) return;

            double amount;
            try {
                amount = Double.parseDouble(String.valueOf(amountInput.getText()).trim());
            } catch (NumberFormatException e) {
                amount = 0;
            }

            viewModel.createExpense(
                    availableProperties.get(selected).localId,
                    category,
                    String.valueOf(descriptionInput.getText()).trim(),
                    amount,
                    "landlord"
            );
        });
    }
}
