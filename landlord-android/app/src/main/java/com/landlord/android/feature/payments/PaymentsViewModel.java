package com.landlord.android.feature.payments;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.landlord.android.feature.tenants.TenantEntity;
import java.util.List;

public class PaymentsViewModel extends AndroidViewModel {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public PaymentsViewModel(@NonNull Application application) {
        super(application);
        invoiceRepository = new InvoiceRepository(application);
        paymentRepository = new PaymentRepository(application);
    }

    public LiveData<List<InvoiceEntity>> invoices() {
        return invoiceRepository.observeAll();
    }

    public LiveData<List<TenantEntity>> tenants() {
        return invoiceRepository.tenants();
    }

    public void generateInvoice(String tenantLocalId, double utilitiesTotal) {
        invoiceRepository.generateInvoice(tenantLocalId, utilitiesTotal);
    }

    public void recordPayment(String invoiceLocalId, String tenantLocalId, double amount, String method) {
        paymentRepository.recordPayment(invoiceLocalId, tenantLocalId, amount, method);
    }
}
