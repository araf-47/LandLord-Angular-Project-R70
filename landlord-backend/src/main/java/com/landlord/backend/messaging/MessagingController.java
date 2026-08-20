package com.landlord.backend.messaging;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class MessagingController {

    private final ConversationRepository conversations;
    private final MessageRepository messages;

    public MessagingController(ConversationRepository conversations, MessageRepository messages) {
        this.conversations = conversations;
        this.messages = messages;
    }

    @GetMapping("/api/conversations")
    public List<Conversation> list(@RequestParam(required = false) Long tenantId) {
        if (tenantId != null) return conversations.findByTenantId(tenantId);
        return conversations.findAll();
    }

    public record NewConversationRequest(Long tenantId, String withName) {}

    @PostMapping("/api/conversations")
    public ResponseEntity<Conversation> create(@RequestBody NewConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setTenantId(request.tenantId());
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
