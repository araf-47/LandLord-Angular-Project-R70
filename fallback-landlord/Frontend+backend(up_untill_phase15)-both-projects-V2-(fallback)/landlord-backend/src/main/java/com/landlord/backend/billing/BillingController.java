package com.landlord.backend.billing;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class BillingController {

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final BillingService billingService;

    public BillingController(InvoiceRepository invoices, PaymentRepository payments, BillingService billingService) {
        this.invoices = invoices;
        this.payments = payments;
        this.billingService = billingService;
    }

    @GetMapping("/api/invoices")
    public List<Invoice> invoicesForTenant(@RequestParam(required = false) Long tenantId, @RequestParam(required = false) String period) {
        if (tenantId != null) return invoices.findByTenantId(tenantId);
        if (period != null) return invoices.findByPeriod(period);
        return invoices.findAll();
    }

    @GetMapping("/api/payments")
    public List<Payment> paymentsForTenant(@RequestParam(required = false) Long tenantId) {
        if (tenantId != null) return payments.findByTenantId(tenantId);
        return payments.findAll();
    }

    public record GenerateInvoiceRequest(Long tenantId, Double utilitiesTotal) {}

    @PostMapping("/api/invoices/generate")
    public ResponseEntity<Invoice> generate(@RequestBody GenerateInvoiceRequest request) {
        Invoice invoice = billingService.generateInvoice(request.tenantId(), request.utilitiesTotal());
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    public record RecordPaymentRequest(Long tenantId, Long invoiceId, Double amount, String method) {}

    @PostMapping("/api/payments")
    public ResponseEntity<Payment> recordPayment(@RequestBody RecordPaymentRequest request) {
        Invoice invoice = invoices.findById(request.invoiceId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        double newBalance = Math.max(0, invoice.getBalance() - request.amount());
        invoice.setBalance(newBalance);
        invoice.setStatus(newBalance <= 0 ? "paid" : "partial");
        invoices.save(invoice);

        Payment payment = new Payment();
        payment.setTenantId(request.tenantId());
        payment.setInvoiceId(request.invoiceId());
        payment.setAmount(request.amount());
        payment.setMethod(request.method() == null ? "cash" : request.method());
        payment.setStatus("confirmed");

        return ResponseEntity.status(HttpStatus.CREATED).body(payments.save(payment));
    }

    @GetMapping("/api/tenants/{tenantId}/outstanding-balance")
    public double outstandingBalance(@PathVariable Long tenantId) {
        return invoices.findByTenantIdAndStatusNot(tenantId, "paid").stream()
            .mapToDouble(Invoice::getBalance)
            .sum();
    }
}
