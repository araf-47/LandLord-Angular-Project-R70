package com.landlord.backend.billing;

import com.landlord.backend.tenant.Tenant;
import com.landlord.backend.tenant.TenantRepository;
import com.landlord.backend.unit.Unit;
import com.landlord.backend.unit.UnitRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final TenantRepository tenants;
    private final UnitRepository units;

    public BillingController(InvoiceRepository invoices, PaymentRepository payments, TenantRepository tenants, UnitRepository units) {
        this.invoices = invoices;
        this.payments = payments;
        this.tenants = tenants;
        this.units = units;
    }

    @GetMapping("/api/invoices")
    public List<Invoice> invoicesForTenant(@RequestParam(required = false) Long tenantId, @RequestParam(required = false) String period) {
        if (tenantId != null) return invoices.findByTenantId(tenantId);
        if (period != null) return invoices.findByPeriod(period);
        return invoices.findAll();
    }

    @GetMapping("/api/payments")
    public List<Payment> paymentsForTenant(@RequestParam Long tenantId) {
        return payments.findByTenantId(tenantId);
    }

    public record GenerateInvoiceRequest(Long tenantId, Double utilitiesTotal) {}

    @PostMapping("/api/invoices/generate")
    public ResponseEntity<Invoice> generate(@RequestBody GenerateInvoiceRequest request) {
        Tenant tenant = tenants.findById(request.tenantId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        if (tenant.getUnitId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant has no assigned unit");
        }
        Unit unit = units.findById(tenant.getUnitId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

        String period = YearMonth.now().format(PERIOD_FORMAT);
        double prevUnpaidRolled = invoices.findByTenantIdAndStatusNot(tenant.getId(), "paid").stream()
            .mapToDouble(Invoice::getBalance)
            .sum();
        double rent = unit.getRent() == null ? 0 : unit.getRent();
        double utilitiesTotal = request.utilitiesTotal() == null ? 0 : request.utilitiesTotal();
        double amount = rent + utilitiesTotal + prevUnpaidRolled;

        Invoice invoice = new Invoice();
        invoice.setTenantId(tenant.getId());
        invoice.setUnitId(unit.getId());
        invoice.setPeriod(period);
        invoice.setRent(rent);
        invoice.setUtilitiesTotal(utilitiesTotal);
        invoice.setPrevUnpaidRolled(prevUnpaidRolled);
        invoice.setAmount(amount);
        invoice.setBalance(amount);
        invoice.setStatus("unpaid");
        invoice.setDueDate(LocalDate.now().withDayOfMonth(Math.min(5, LocalDate.now().lengthOfMonth())));

        return ResponseEntity.status(HttpStatus.CREATED).body(invoices.save(invoice));
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
