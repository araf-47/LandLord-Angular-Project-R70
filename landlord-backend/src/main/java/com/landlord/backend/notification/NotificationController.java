package com.landlord.backend.notification;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class NotificationController {

    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/api/notifications")
    public List<Notification> list(@RequestParam(required = false) Long tenantId) {
        if (tenantId != null) return notifications.findByTenantId(tenantId);
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
