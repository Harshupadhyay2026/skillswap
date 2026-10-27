package com.skillswap.modules.notification.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.common.PageResponse;
import com.skillswap.modules.notification.dto.NotificationResponse;
import com.skillswap.modules.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** GET /api/notifications?page=&size= */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> list(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Notifications fetched",
                notificationService.list(principal.getUsername(), page, size)));
    }

    /** GET /api/notifications/unread-count — for the navbar bell. */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Unread count fetched",
                Map.of("count", notificationService.unreadCount(principal.getUsername()))));
    }

    /** GET /api/notifications/email-status — SMTP readiness for the UI. */
    @GetMapping("/email-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> emailStatus(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Email status fetched",
                notificationService.emailStatus(principal.getUsername())));
    }

    /** POST /api/notifications/test-email — verify SMTP with a real message. */
    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestEmail(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Test email sent",
                notificationService.sendTestEmail(principal.getUsername())));
    }

    /** PUT /api/notifications/{id}/read */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Notification marked as read",
                notificationService.markRead(principal.getUsername(), id)));
    }

    /** PUT /api/notifications/read-all */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(
            @AuthenticationPrincipal UserDetails principal) {
        int updated = notificationService.markAllRead(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                "All notifications marked as read",
                Map.of("updated", updated)));
    }
}
