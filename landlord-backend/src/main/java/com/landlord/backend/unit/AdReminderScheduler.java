package com.landlord.backend.unit;

import com.landlord.backend.notification.Notification;
import com.landlord.backend.notification.NotificationRepository;
import com.landlord.backend.property.Property;
import com.landlord.backend.property.PropertyRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Phase 16.3: flags a unit whose BariVara ad has sat vacant too long without a
 * booking. No BariVara-side notification exists for landlord-linked ads, so this
 * writes a landlord-facing (tenantId=null) Notification instead — the same
 * one-shot-per-vacancy-stretch model as Unit.vacantSince/adReminderSentAt, cleared
 * whenever the unit is reoccupied (UnitController, TenantController, MarketplaceController).
 */
@Component
public class AdReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdReminderScheduler.class);
    private static final Duration STALE_AFTER = Duration.ofDays(14);

    private final UnitRepository units;
    private final PropertyRepository properties;
    private final NotificationRepository notifications;

    public AdReminderScheduler(UnitRepository units, PropertyRepository properties, NotificationRepository notifications) {
        this.units = units;
        this.properties = properties;
        this.notifications = notifications;
    }

    /** Once a day, 9am server-local time. */
    @Scheduled(cron = "0 0 9 * * *")
    public void flagStaleAds() {
        Instant staleCutoff = Instant.now().minus(STALE_AFTER);
        int flagged = 0;
        for (Unit unit : units.findByStatusAndAdPausedFalse("vacant")) {
            if (unit.getVacantSince() == null || unit.getAdReminderSentAt() != null) continue;
            if (unit.getVacantSince().isAfter(staleCutoff)) continue;

            String propertyName = properties.findById(unit.getPropertyId()).map(Property::getName).orElse("a property");
            Notification notification = new Notification();
            notification.setTenantId(null);
            notification.setType("ad-expiry");
            notification.setTitle("Ad still unfilled: " + propertyName + " / " + unit.getUnitNumber());
            notification.setBody("Unit " + unit.getUnitNumber() + " at " + propertyName
                + " has been vacant and listed for " + STALE_AFTER.toDays() + "+ days with no booking. "
                + "Consider reviewing the rent, photo, or reposting the ad.");
            notifications.save(notification);

            unit.setAdReminderSentAt(Instant.now());
            units.save(unit);
            flagged++;
        }
        log.info("Ad-expiry reminder sweep: {} unit(s) flagged", flagged);
    }
}
