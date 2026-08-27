package com.landlord.backend.notification;

import com.idb.auth.model.User;
import com.landlord.backend.tenant.TenantRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NotificationController {

    private final NotificationRepository notifications;
    private final TenantRepository tenants;

    public NotificationController(NotificationRepository notifications, TenantRepository tenants) {
        this.notifications = notifications;
        this.tenants = tenants;
    }

    /** See BillingController.effectiveTenantId - same reasoning, same fix. */
    private Long effectiveTenantId(User principal, Long requestedTenantId) {
        boolean isLandlord = principal.getRoles().stream().anyMatch(r -> "LANDLORD".equals(r.getName()));
        if (isLandlord) {
            return requestedTenantId;
        }
        return tenants.findFirstByAuthUserId(principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tenant record linked to this account"))
            .getId();
    }

    /** `audience=landlord` (Phase 16.3) returns tenantId-less notifications — this
     *  app has no landlord-user table, so a null tenantId doubles as "for the
     *  landlord", same trick Notification already used for its optional field. */
    @GetMapping("/api/notifications")
    public List<Notification> list(@AuthenticationPrincipal User principal,
            @RequestParam(required = false) Long tenantId, @RequestParam(required = false) String audience) {
        Long effective = effectiveTenantId(principal, tenantId);
        if (effective != null) return notifications.findByTenantId(effective);
        if ("landlord".equals(audience)) return notifications.findByTenantIdIsNull();
        return notifications.findAll();
    }

    public record NewNotificationRequest(Long tenantId, String type, String title, String body) {}

    @PostMapping("/api/notifications")
    public ResponseEntity<Notification> create(@RequestBody NewNotificationRequest request) {
        Notification notification = new Notification();
        notification.setTenantId(request.tenantId());
        notification.setType(request.type());
        notification.setTitle(request.title());
        notification.setBody(request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(notifications.save(notification));
    }

    @PutMapping("/api/notifications/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable Long id) {
        Notification notification = notifications.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        return ResponseEntity.ok(notifications.save(notification));
    }

    @DeleteMapping("/api/notifications/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notifications.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
