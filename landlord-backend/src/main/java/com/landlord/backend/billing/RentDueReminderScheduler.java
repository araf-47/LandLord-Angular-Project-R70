package com.landlord.backend.billing;

import com.landlord.backend.notification.Notification;
import com.landlord.backend.notification.NotificationRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Phase 16.2: reminds a tenant once their invoice's due date has arrived (or
 * passed) and it's still unpaid/partial. One-shot per invoice via
 * `reminderSentAt` — a daily overdue nag isn't in scope here, just the single
 * "this is due" notification the tenant's Notifications page already renders.
 */
@Component
public class RentDueReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(RentDueReminderScheduler.class);

    private final InvoiceRepository invoices;
    private final NotificationRepository notifications;

    public RentDueReminderScheduler(InvoiceRepository invoices, NotificationRepository notifications) {
        this.invoices = invoices;
        this.notifications = notifications;
    }

    /** Once a day, 8am server-local time. */
    @Scheduled(cron = "0 0 8 * * *")
    public void remindDueInvoices() {
        int reminded = 0;
        for (Invoice invoice : invoices.findByStatusNotAndReminderSentAtIsNullAndDueDateLessThanEqual("paid", LocalDate.now())) {
            if (invoice.getTenantId() == null) continue;

            Notification notification = new Notification();
            notification.setTenantId(invoice.getTenantId());
            notification.setType("rent-due");
            notification.setTitle("Rent due: " + invoice.getPeriod());
            notification.setBody("Your bill for " + invoice.getPeriod() + " (balance: " + invoice.getBalance()
                + ") was due " + invoice.getDueDate() + ". Please arrange payment if you haven't already.");
            notifications.save(notification);

            invoice.setReminderSentAt(Instant.now());
            invoices.save(invoice);
            reminded++;
        }
        log.info("Rent-due reminder sweep: {} tenant(s) reminded", reminded);
    }
}
