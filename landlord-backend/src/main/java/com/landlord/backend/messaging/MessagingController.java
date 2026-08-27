package com.landlord.backend.messaging;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MessagingController {

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final TenantRepository tenants;

    public MessagingController(ConversationRepository conversations, MessageRepository messages, TenantRepository tenants) {
        this.conversations = conversations;
        this.messages = messages;
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

    @GetMapping("/api/conversations")
    public List<Conversation> list(@AuthenticationPrincipal User principal, @RequestParam(required = false) Long tenantId) {
        Long effective = effectiveTenantId(principal, tenantId);
        if (effective != null) return conversations.findByTenantId(effective);
        return conversations.findAll();
    }

    public record NewConversationRequest(Long tenantId, String withName) {}

    @PostMapping("/api/conversations")
    public ResponseEntity<Conversation> create(@AuthenticationPrincipal User principal,
            @RequestBody NewConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setTenantId(request.tenantId() == null ? null : effectiveTenantId(principal, request.tenantId()));
        conversation.setWithName(request.withName());
        return ResponseEntity.status(HttpStatus.CREATED).body(conversations.save(conversation));
    }

    @GetMapping("/api/conversations/{id}/messages")
    public List<Message> messagesFor(@PathVariable Long id) {
        conversations.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        return messages.findByConversationId(id);
    }

    public record NewMessageRequest(String senderRole, String text) {}

    @PostMapping("/api/conversations/{id}/messages")
    public ResponseEntity<Message> send(@PathVariable Long id, @RequestBody NewMessageRequest request) {
        conversations.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        Message message = new Message();
        message.setConversationId(id);
        message.setSenderRole(request.senderRole());
        message.setText(request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(messages.save(message));
    }
}
