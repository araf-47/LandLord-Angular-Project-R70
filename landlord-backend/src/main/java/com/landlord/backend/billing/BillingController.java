package com.landlord.backend.billing;

import com.idb.auth.model.User;
import com.landlord.backend.tenant.TenantRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class BillingController {

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final BillingService billingService;
    private final TenantRepository tenants;

    public BillingController(InvoiceRepository invoices, PaymentRepository payments, BillingService billingService,
            TenantRepository tenants) {
        this.invoices = invoices;
        this.payments = payments;
        this.billingService = billingService;
        this.tenants = tenants;
    }

    /**
     * A TENANT-role caller's own tenant id always wins over whatever id was
     * passed in the request - permissions.json only checks role-vs-URL, not
     * row ownership, so without this a tenant could read/pay against any other
     * tenant's id just by changing a query param. LANDLORD callers keep using
     * the id they passed in, since the landlord legitimately looks up any tenant.
     */
    private Long effectiveTenantId(User principal, Long requestedTenantId) {
        boolean isLandlord = principal.getRoles().stream().anyMatch(r -> "LANDLORD".equals(r.getName()));
        if (isLandlord) {
            return requestedTenantId;
        }
        return tenants.findFirstByAuthUserId(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tenant record linked to this account"))
            .getId();
    }

    @GetMapping("/api/invoices")
    public List<Invoice> invoicesForTenant(@AuthenticationPrincipal User principal,
            @RequestParam(required = false) Long tenantId, @RequestParam(required = false) String period) {
        Long effective = effectiveTenantId(principal, tenantId);
        if (effective != null) return invoices.findByTenantId(effective);
        if (period != null) return invoices.findByPeriod(period);
        return invoices.findAll();
    }

    @GetMapping("/api/payments")
    public List<Payment> paymentsForTenant(@AuthenticationPrincipal User principal,
            @RequestParam(required = false) Long tenantId) {
        Long effective = effectiveTenantId(principal, tenantId);
        if (effective != null) return payments.findByTenantId(effective);
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
    public ResponseEntity<Payment> recordPayment(@AuthenticationPrincipal User principal,
            @RequestBody RecordPaymentRequest request) {
        Long effective = effectiveTenantId(principal, request.tenantId());

        Invoice invoice = invoices.findById(request.invoiceId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        double newBalance = Math.max(0, invoice.getBalance() - request.amount());
        invoice.setBalance(newBalance);
        invoice.setStatus(newBalance <= 0 ? "paid" : "partial");
        invoices.save(invoice);

        Payment payment = new Payment();
        payment.setTenantId(effective);
        payment.setInvoiceId(request.invoiceId());
        payment.setAmount(request.amount());
        payment.setMethod(request.method() == null ? "cash" : request.method());
        payment.setStatus("confirmed");

        return ResponseEntity.status(HttpStatus.CREATED).body(payments.save(payment));
    }

    @GetMapping("/api/tenants/{tenantId}/outstanding-balance")
    public double outstandingBalance(@AuthenticationPrincipal User principal, @PathVariable Long tenantId) {
        Long effective = effectiveTenantId(principal, tenantId);
        return invoices.findByTenantIdAndStatusNot(effective, "paid").stream()
            .mapToDouble(Invoice::getBalance)
            .sum();
    }
}
