package com.landlord.android.feature.reports;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.landlord.android.core.common.AppExecutors;
import com.landlord.android.core.common.Result;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.network.NetworkModule;
import com.landlord.android.feature.payments.InvoiceEntity;
import com.landlord.android.feature.properties.PropertyEntity;
import com.landlord.android.feature.tenants.TenantEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsViewModel extends AndroidViewModel {

    private final ReportApiService api;
    private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        api = NetworkModule.getRetrofit(application).create(ReportApiService.class);
    }

    public LiveData<List<PropertyEntity>> properties() {
        return AppDatabase.getInstance(getApplication()).propertyDao().observeAll();
    }

    public LiveData<List<TenantEntity>> tenants() {
        return AppDatabase.getInstance(getApplication()).tenantDao().observeAll();
    }

    public LiveData<Result<String>> viewJson(ReportType type, String startDate, String endDate,
                                              Long propertyId, Long tenantId, String category) {
        MutableLiveData<Result<String>> result = new MutableLiveData<>(Result.loading());
        Call<ResponseBody> call = buildJsonCall(type, startDate, endDate, propertyId, tenantId, category);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    result.postValue(Result.error("HTTP " + response.code()));
                    return;
                }
                try {
                    String raw = response.body().string();
                    JsonElement parsed = JsonParser.parseString(raw);
                    result.postValue(Result.success(prettyGson.toJson(parsed)));
                } catch (IOException e) {
                    result.postValue(Result.error("Couldn't read response"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                result.postValue(Result.error(t.getMessage() != null ? t.getMessage() : "Network error"));
            }
        });

        return result;
    }

    public LiveData<Result<List<TenantDueRow>>> viewTenantDues() {
        MutableLiveData<Result<List<TenantDueRow>>> result = new MutableLiveData<>(Result.loading());
        AppExecutors.DB.execute(() -> result.postValue(Result.success(buildTenantDuesRows())));
        return result;
    }

    private List<TenantDueRow> buildTenantDuesRows() {
        AppDatabase db = AppDatabase.getInstance(getApplication());
        List<InvoiceEntity> invoices = db.invoiceDao().getAllSync();
        List<TenantEntity> tenants = db.tenantDao().getAllSync();

        java.util.Map<String, Double> balanceByTenant = new java.util.HashMap<>();
        java.util.Map<String, Boolean> hasUnpaidByTenant = new java.util.HashMap<>();
        for (InvoiceEntity invoice : invoices) {
            if ("paid".equals(invoice.status) || invoice.tenantLocalId == null || invoice.balance == null) continue;
            balanceByTenant.merge(invoice.tenantLocalId, invoice.balance, Double::sum);
            if ("unpaid".equals(invoice.status)) hasUnpaidByTenant.put(invoice.tenantLocalId, true);
        }

        List<TenantDueRow> rows = new ArrayList<>();
        for (TenantEntity tenant : tenants) {
            if (!"active".equals(tenant.status)) continue;
            Double due = balanceByTenant.get(tenant.localId);
            if (due == null || due <= 0) continue;
            String status = Boolean.TRUE.equals(hasUnpaidByTenant.get(tenant.localId)) ? "unpaid" : "partial";
            rows.add(new TenantDueRow(tenant.name, due, status));
        }
        Collections.sort(rows, (a, b) -> Double.compare(b.totalDue, a.totalDue));
        return rows;
    }

    public Call<ResponseBody> buildExportCall(ReportType type, boolean pdf, String startDate, String endDate,
                                               Long propertyId, Long tenantId, String category) {
        switch (type) {
            case INCOME_STATEMENT:
                return pdf ? api.incomeStatementPdf(startDate, endDate, propertyId)
                        : api.incomeStatementXlsx(startDate, endDate, propertyId);
            case EXPENSE_REPORT:
                return pdf ? api.expenseReportPdf(startDate, endDate, propertyId, category)
                        : api.expenseReportXlsx(startDate, endDate, propertyId, category);
            case OCCUPANCY_REPORT:
                return pdf ? api.occupancyReportPdf(propertyId) : api.occupancyReportXlsx(propertyId);
            case TENANT_LEDGER:
                return pdf ? api.tenantLedgerPdf(tenantId, startDate, endDate)
                        : api.tenantLedgerXlsx(tenantId, startDate, endDate);
            case TENANT_DUES:
                throw new IllegalArgumentException("Tenant dues has no backend export - caller must check hasJsonView/type first");
            case FULL_REPORT:
            default:
                return pdf ? api.fullReportPdf(startDate, endDate, propertyId)
                        : api.fullReportXlsx(startDate, endDate, propertyId);
        }
    }

    private Call<ResponseBody> buildJsonCall(ReportType type, String startDate, String endDate,
                                              Long propertyId, Long tenantId, String category) {
        switch (type) {
            case INCOME_STATEMENT:
                return api.incomeStatement(startDate, endDate, propertyId);
            case EXPENSE_REPORT:
                return api.expenseReport(startDate, endDate, propertyId, category);
            case OCCUPANCY_REPORT:
                return api.occupancyReport(propertyId);
            case TENANT_LEDGER:
            default:
                return api.tenantLedger(tenantId, startDate, endDate);
        }
    }
}
