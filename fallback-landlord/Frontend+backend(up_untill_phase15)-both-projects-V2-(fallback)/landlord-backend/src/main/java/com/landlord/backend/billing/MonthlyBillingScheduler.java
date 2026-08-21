package com.landlord.backend.billing;

import com.landlord.backend.tenant.Tenant;
import com.landlord.backend.tenant.TenantRepository;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Phase 10.3: auto-generates this month's rent invoice for every active,
 * unit-assigned tenant who doesn't already have one — same rule the manual
 * "Generate bills" button (generate-bills.component.ts) uses, just on a
 * schedule instead of a click. No utilities total to pass here (that's a
 * per-bill landlord input), so auto-generated bills carry rent + rollover
 * only; the landlord can still edit/add utilities before anyone pays.
 */
@Component
public class MonthlyBillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyBillingScheduler.class);
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TenantRepository tenants;
    private final InvoiceRepository invoices;
    private final BillingService billingService;

    public MonthlyBillingScheduler(TenantRepository tenants, InvoiceRepository invoices, BillingService billingService) {
        this.tenants = tenants;
        this.invoices = invoices;
        this.billingService = billingService;
    }

    /** Midnight on the 1st of every month, server-local time. */
    @Scheduled(cron = "0 0 0 1 * *")
    public void generateMonthlyBills() {
        String period = YearMonth.now().format(PERIOD_FORMAT);
        Set<Long> alreadyBilled = new HashSet<>();
        for (Invoice invoice : invoices.findByPeriod(period)) {
            alreadyBilled.add(invoice.getTenantId());
        }

        int generated = 0;
        for (Tenant tenant : tenants.findAll()) {
            boolean due = "active".equals(tenant.getStatus()) && tenant.getUnitId() != null && !alreadyBilled.contains(tenant.getId());
            if (!due) continue;
            try {
                billingService.generateInvoice(tenant.getId(), null);
                generated++;
            } catch (Exception e) {
                log.error("Auto bill generation failed for tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
        log.info("Monthly bill generation for {}: {} invoice(s) created", period, generated);
    }
}
