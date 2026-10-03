package com.landlord.android.feature.reports;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.landlord.android.R;
import com.landlord.android.core.common.Result;
import com.landlord.android.feature.properties.PropertyEntity;
import com.landlord.android.feature.reports.ReportFileDownloader;
import com.landlord.android.feature.tenants.TenantEntity;
import com.landlord.android.ui.widgets.RequiresInternetBanner;
import java.util.ArrayList;
import java.util.List;

public class ReportsFragment extends Fragment {

    private ReportsViewModel viewModel;
    private List<PropertyEntity> availableProperties = new ArrayList<>();
    private List<TenantEntity> availableTenants = new ArrayList<>();

    private Spinner typeSpinner;
    private View dateRangeRow;
    private View propertyRow;
    private View tenantRow;
    private View categoryRow;
    private Spinner propertySpinner;
    private Spinner tenantSpinner;
    private TextInputEditText startDateInput;
    private TextInputEditText endDateInput;
    private TextInputEditText categoryInput;
    private ProgressBar progress;
    private TextView output;
    private View jsonCard;
    private View tenantDuesCard;
    private TextView tenantDuesSummary;
    private TextView tenantDuesEmpty;
    private RecyclerView tenantDuesList;
    private TenantDuesAdapter tenantDuesAdapter;

    @Nullable
    @Override
    public View onCreateView(android.view.LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication()))
                .get(ReportsViewModel.class);

        RequiresInternetBanner banner = view.findViewById(R.id.reports_offline_banner);
        banner.observe(getViewLifecycleOwner());

        typeSpinner = view.findViewById(R.id.input_report_type);
        dateRangeRow = view.findViewById(R.id.date_range_row);
        propertyRow = view.findViewById(R.id.property_row);
        tenantRow = view.findViewById(R.id.tenant_row);
        categoryRow = view.findViewById(R.id.category_row);
        propertySpinner = view.findViewById(R.id.input_property);
        tenantSpinner = view.findViewById(R.id.input_tenant);
        startDateInput = view.findViewById(R.id.input_start_date);
        endDateInput = view.findViewById(R.id.input_end_date);
        categoryInput = view.findViewById(R.id.input_category);
        progress = view.findViewById(R.id.report_progress);
        output = view.findViewById(R.id.report_json_output);
        jsonCard = view.findViewById(R.id.report_json_card);
        tenantDuesCard = view.findViewById(R.id.tenant_dues_card);
        tenantDuesSummary = view.findViewById(R.id.tenant_dues_summary);
        tenantDuesEmpty = view.findViewById(R.id.tenant_dues_empty);
        tenantDuesList = view.findViewById(R.id.tenant_dues_list);
        tenantDuesAdapter = new TenantDuesAdapter();
        tenantDuesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        tenantDuesList.setAdapter(tenantDuesAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, labelsOf(ReportType.values()));
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                updateFieldVisibility(ReportType.values()[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        updateFieldVisibility(ReportType.values()[0]);

        viewModel.properties().observe(getViewLifecycleOwner(), props -> {
            availableProperties = new ArrayList<>();
            for (PropertyEntity p : props) if (p.sync.serverId != null) availableProperties.add(p);
            List<String> labels = new ArrayList<>();
            labels.add("(all properties)");
            for (PropertyEntity p : availableProperties) labels.add(p.name);
            propertySpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, labels));
        });

        viewModel.tenants().observe(getViewLifecycleOwner(), tenants -> {
            availableTenants = new ArrayList<>();
            for (TenantEntity t : tenants) if (t.sync.serverId != null) availableTenants.add(t);
            List<String> labels = new ArrayList<>();
            for (TenantEntity t : availableTenants) labels.add(t.name);
            tenantSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, labels));
        });

        view.findViewById(R.id.view_report_button).setOnClickListener(v -> onView());
        view.findViewById(R.id.export_pdf_button).setOnClickListener(v -> onExport(true));
        view.findViewById(R.id.export_xlsx_button).setOnClickListener(v -> onExport(false));
    }

    private List<String> labelsOf(ReportType[] types) {
        List<String> labels = new ArrayList<>();
        for (ReportType t : types) labels.add(t.label);
        return labels;
    }

    private void updateFieldVisibility(ReportType type) {
        dateRangeRow.setVisibility(type.needsDateRange ? View.VISIBLE : View.GONE);
        propertyRow.setVisibility(type.needsProperty ? View.VISIBLE : View.GONE);
        tenantRow.setVisibility(type.needsTenant ? View.VISIBLE : View.GONE);
        categoryRow.setVisibility(type.needsCategory ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.view_report_button).setEnabled(type.hasJsonView);
        jsonCard.setVisibility(View.GONE);
        tenantDuesCard.setVisibility(View.GONE);
    }

    private ReportType selectedType() {
        return ReportType.values()[typeSpinner.getSelectedItemPosition()];
    }

    @Nullable
    private Long selectedPropertyId() {
        int pos = propertySpinner.getSelectedItemPosition();
        if (pos <= 0 || pos - 1 >= availableProperties.size()) return null;
        return availableProperties.get(pos - 1).sync.serverId;
    }

    @Nullable
    private Long selectedTenantId() {
        int pos = tenantSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= availableTenants.size()) return null;
        return availableTenants.get(pos).sync.serverId;
    }

    private String startDate() {
        return String.valueOf(startDateInput.getText()).trim();
    }

    private String endDate() {
        return String.valueOf(endDateInput.getText()).trim();
    }

    private String category() {
        String c = String.valueOf(categoryInput.getText()).trim();
        return c.isEmpty() ? null : c;
    }

    private void onView() {
        ReportType type = selectedType();
        if (!type.hasJsonView) {
            Snackbar.make(requireView(), "This report only supports PDF/XLSX export", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (type == ReportType.TENANT_DUES) {
            onViewTenantDues();
            return;
        }

        jsonCard.setVisibility(View.VISIBLE);
        tenantDuesCard.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        output.setText("");

        viewModel.viewJson(type, startDate(), endDate(), selectedPropertyId(), selectedTenantId(), category())
                .observe(getViewLifecycleOwner(), result -> {
                    if (result instanceof Result.Loading) return;
                    progress.setVisibility(View.GONE);
                    if (result instanceof Result.Success) {
                        output.setText(((Result.Success<String>) result).data);
                    } else if (result instanceof Result.Error) {
                        output.setText("Error: " + ((Result.Error<?>) result).message);
                    }
                });
    }

    private void onViewTenantDues() {
        jsonCard.setVisibility(View.GONE);
        tenantDuesCard.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);

        viewModel.viewTenantDues().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) return;
            progress.setVisibility(View.GONE);
            if (!(result instanceof Result.Success)) return;

            List<TenantDueRow> rows = ((Result.Success<List<TenantDueRow>>) result).data;
            tenantDuesAdapter.submitList(rows);
            tenantDuesEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            tenantDuesList.setVisibility(rows.isEmpty() ? View.GONE : View.VISIBLE);

            double total = 0;
            for (TenantDueRow row : rows) total += row.totalDue;
            tenantDuesSummary.setText(rows.isEmpty()
                    ? "All active tenants are paid up"
                    : String.format(java.util.Locale.getDefault(),
                            "%d tenant%s owe a total of %.2f", rows.size(), rows.size() == 1 ? "" : "s", total));
        });
    }

    private void onExport(boolean pdf) {
        ReportType type = selectedType();
        if (type == ReportType.TENANT_DUES) {
            Snackbar.make(requireView(), "Tenant dues is view-only for now - tap \"View\"", Snackbar.LENGTH_LONG).show();
            return;
        }
        progress.setVisibility(View.VISIBLE);

        retrofit2.Call<okhttp3.ResponseBody> call = viewModel.buildExportCall(
                type, pdf, startDate(), endDate(), selectedPropertyId(), selectedTenantId(), category());

        String fileName = type.name().toLowerCase() + "-" + System.currentTimeMillis() + (pdf ? ".pdf" : ".xlsx");
        String mimeType = pdf ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        ReportFileDownloader.download(requireContext(), call, fileName, mimeType, new ReportFileDownloader.Callback() {
            @Override
            public void onSuccess(android.net.Uri uri, String mimeType) {
                progress.setVisibility(View.GONE);
                ReportFileDownloader.openFile(requireContext(), uri, mimeType);
            }

            @Override
            public void onError(String message) {
                progress.setVisibility(View.GONE);
                Snackbar.make(requireView(), "Export failed: " + message, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
