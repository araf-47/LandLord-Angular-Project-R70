package com.landlord.android.feature.payments;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.landlord.android.core.common.AppExecutors;
import com.landlord.android.feature.tenants.TenantEntity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PaymentsViewModel extends AndroidViewModel {

    /** Matches the backend's Invoice.period format (yyyy-MM), same as the web
     *  generate-bills.component.ts periodKey() - bills only ever show for the
     *  current month, same as the website. */
    private static final String CURRENT_PERIOD =
            new SimpleDateFormat("yyyy-MM", Locale.US).format(new java.util.Date());

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    private final MediatorLiveData<List<BillRow>> bills = new MediatorLiveData<>();
    private List<InvoiceEntity> lastInvoices = new ArrayList<>();
    private List<TenantEntity> lastTenants = new ArrayList<>();

    public PaymentsViewModel(@NonNull Application application) {
        super(application);
        invoiceRepository = new InvoiceRepository(application);
        paymentRepository = new PaymentRepository(application);

        bills.addSource(invoiceRepository.observeAll(), items -> { lastInvoices = items; recomputeBills(); });
        bills.addSource(invoiceRepository.tenants(), items -> { lastTenants = items; recomputeBills(); });
    }

    public LiveData<List<InvoiceEntity>> invoices() {
        return invoiceRepository.observeAll();
    }

    public LiveData<List<TenantEntity>> tenants() {
        return invoiceRepository.tenants();
    }

    /** This month's bills, one row per invoice already generated for the current
     *  period - the Android counterpart to the web "Monthly Bills" table. */
    public LiveData<List<BillRow>> bills() {
        return bills;
    }

    private void recomputeBills() {
        List<BillRow> result = new ArrayList<>();
        for (InvoiceEntity invoice : lastInvoices) {
            if (!CURRENT_PERIOD.equals(invoice.period)) continue;
            result.add(new BillRow(
                    invoice.localId,
                    tenantName(invoice.tenantLocalId),
                    invoice.rent != null ? invoice.rent : 0,
                    invoice.utilitiesTotal,
                    invoice.prevUnpaidRolled != null ? invoice.prevUnpaidRolled : 0,
                    invoice.balance != null ? invoice.balance : 0,
                    invoice.amount != null ? invoice.amount : 0,
                    invoice.status));
        }
        bills.setValue(result);
    }

    private String tenantName(String tenantLocalId) {
        if (tenantLocalId == null) return "—";
        for (TenantEntity t : lastTenants) {
            if (t.localId.equals(tenantLocalId)) return t.name;
        }
        return "—";
    }

    /** Active tenants missing a bill for the current period yet - generates one
     *  for each with utilitiesTotal 0, same default as the web "Generate bills for
     *  tenants missing one this month" button. */
    public void generateMissingBills() {
        AppExecutors.DB.execute(() -> {
            java.util.Set<String> billed = new java.util.HashSet<>();
            for (InvoiceEntity invoice : lastInvoices) {
                if (CURRENT_PERIOD.equals(invoice.period) && invoice.tenantLocalId != null) {
                    billed.add(invoice.tenantLocalId);
                }
            }
            for (TenantEntity tenant : lastTenants) {
                if ("active".equals(tenant.status) && tenant.unitLocalId != null && !billed.contains(tenant.localId)) {
                    invoiceRepository.generateInvoice(tenant.localId, 0);
                }
            }
        });
    }

    /** Active tenants, for the Record Payment tenant picker - matches the web
     *  receive-payment.component.ts tenant list (active only). */
    public List<TenantEntity> activeTenants() {
        List<TenantEntity> result = new ArrayList<>();
        for (TenantEntity t : lastTenants) {
            if ("active".equals(t.status)) result.add(t);
        }
        return result;
    }

    /** Sum of balance across a tenant's non-paid invoices - the "Total due" shown
     *  in the Record Payment sheet, same computation as the web's totalDue(). */
    public double totalDueForTenant(String tenantLocalId) {
        double sum = 0;
        for (InvoiceEntity invoice : lastInvoices) {
            if (tenantLocalId.equals(invoice.tenantLocalId) && !"paid".equals(invoice.status) && invoice.balance != null) {
                sum += invoice.balance;
            }
        }
        return sum;
    }

    /** A tenant's non-paid invoices, oldest first - payment is applied against
     *  these in order, same FIFO allocation as the web save() method. */
    public List<InvoiceEntity> oldestUnpaidInvoicesForTenant(String tenantLocalId) {
        List<InvoiceEntity> result = new ArrayList<>();
        for (InvoiceEntity invoice : lastInvoices) {
            if (tenantLocalId.equals(invoice.tenantLocalId) && !"paid".equals(invoice.status)) {
                result.add(invoice);
            }
        }
        Collections.sort(result, (a, b) -> Long.compare(a.createdAt, b.createdAt));
        return result;
    }

    public void recordPayment(String invoiceLocalId, String tenantLocalId, double amount, String method) {
        paymentRepository.recordPayment(invoiceLocalId, tenantLocalId, amount, method);
    }
}
