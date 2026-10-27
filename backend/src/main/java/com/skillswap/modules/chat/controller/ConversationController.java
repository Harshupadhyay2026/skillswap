package com.skillswap.modules.chat.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.chat.dto.ConversationResponse;
import com.skillswap.modules.chat.dto.MessageResponse;
import com.skillswap.modules.chat.dto.SendMessageRequest;
import com.skillswap.modules.chat.dto.StartConversationRequest;
import com.skillswap.modules.chat.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /** GET /api/conversations — inbox, most recent first. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> list(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Conversations fetched",
                conversationService.list(principal.getUsername())));
    }

    /** POST /api/conversations — get or create a thread with { userId }. */
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> start(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody StartConversationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Conversation ready",
                conversationService.start(principal.getUsername(), req.getUserId())));
    }

    /** GET /api/conversations/unread-count — total unread messages. */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Unread count fetched",
                Map.of("count", conversationService.unreadCount(principal.getUsername()))));
    }

    /** GET /api/conversations/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationResponse>> get(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Conversation fetched",
                conversationService.get(principal.getUsername(), id)));
    }

    /** GET /api/conversations/{id}/messages?before=&size= */
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> messages(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Messages fetched",
                conversationService.messages(principal.getUsername(), id, before, size)));
    }

    /** POST /api/conversations/{id}/messages */
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Message sent",
                conversationService.send(principal.getUsername(), id, req)));
    }

    /** PUT /api/conversations/{id}/read */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markRead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        int updated = conversationService.markRead(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok(
                "Conversation marked as read",
                Map.of("updated", updated)));
    }
}
