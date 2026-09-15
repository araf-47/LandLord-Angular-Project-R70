package com.landlord.backend.config;

import com.idb.auth.model.User;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parts/auth's JWT carries only the username ("sub" claim) - no role. The
 * frontend needs to know the caller's role right after login (to route to the
 * right dashboard, gate roleGuard, etc.), so this endpoint exposes it.
 */
@RestController
public class AuthMeController {

    public record MeResponse(Long id, String username, List<String> roles, String email, String phone,
            boolean twoFactorEnabled, boolean notifyRentDueEmail, boolean notifyRentDueSms,
            boolean notifyPaymentReceivedEmail, boolean notifyMaintenanceEmail) {}

    @GetMapping("/api/auth/me")
    public MeResponse me(@AuthenticationPrincipal User principal) {
        return new MeResponse(principal.getId(), principal.getUsername(),
                principal.getRoles().stream().map(r -> r.getName()).toList(),
                principal.getEmail(), principal.getPhone(), principal.isTwoFactorEnabled(),
                principal.isNotifyRentDueEmail(), principal.isNotifyRentDueSms(),
                principal.isNotifyPaymentReceivedEmail(), principal.isNotifyMaintenanceEmail());
    }
}
