package com.landlord.android.feature.ledger;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.landlord.android.core.db.AppDatabase;
import com.landlord.android.core.sync.SyncState;
import com.landlord.android.feature.expenses.ExpenseEntity;
import com.landlord.android.feature.payments.InvoiceEntity;
import com.landlord.android.feature.payments.PaymentEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LedgerViewModel extends AndroidViewModel {

    private final MediatorLiveData<List<LedgerEntry>> entries = new MediatorLiveData<>();

    private List<PaymentEntity> lastPayments = new ArrayList<>();
    private List<InvoiceEntity> lastInvoices = new ArrayList<>();
    private List<ExpenseEntity> lastExpenses = new ArrayList<>();

    public LedgerViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);

        LiveData<List<PaymentEntity>> payments = db.paymentDao().observeAll();
        LiveData<List<InvoiceEntity>> invoices = db.invoiceDao().observeAll();
        LiveData<List<ExpenseEntity>> expenses = db.expenseDao().observeAll();

        entries.addSource(payments, p -> { lastPayments = p; recompute(); });
        entries.addSource(invoices, i -> { lastInvoices = i; recompute(); });
        entries.addSource(expenses, e -> { lastExpenses = e; recompute(); });
    }

    public LiveData<List<LedgerEntry>> entries() {
        return entries;
    }

    private void recompute() {
        List<LedgerEntry> result = new ArrayList<>();

        for (PaymentEntity p : lastPayments) {
            result.add(new LedgerEntry(p.createdAt, "Payment",
                    p.method + " payment", p.amount, badge(p.sync.syncState)));
        }
        for (InvoiceEntity i : lastInvoices) {
            result.add(new LedgerEntry(i.createdAt, "Invoice",
                    "Invoice" + (i.period != null ? " " + i.period : "") + " - " + i.status,
                    i.amount != null ? i.amount : 0, badge(i.sync.syncState)));
        }
        for (ExpenseEntity e : lastExpenses) {
            result.add(new LedgerEntry(e.createdAt, "Expense",
                    e.category + (e.description != null && !e.description.isEmpty() ? " - " + e.description : ""),
                    -e.amount, badge(e.sync.syncState)));
        }

        Collections.sort(result, (a, b) -> Long.compare(b.date, a.date));
        entries.setValue(result);
    }

    private String badge(SyncState state) {
        if (state == SyncState.SYNCED) return null;
        return state == SyncState.CONFLICT ? "Sync issue" : "Pending sync";
    }
}
