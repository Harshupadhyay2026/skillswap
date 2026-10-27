package com.skillswap.modules.moderation.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.common.PageResponse;
import com.skillswap.modules.moderation.dto.ModerationStatsResponse;
import com.skillswap.modules.moderation.dto.ReportResponse;
import com.skillswap.modules.moderation.dto.ResolveReportRequest;
import com.skillswap.modules.moderation.dto.RestoreContentRequest;
import com.skillswap.modules.moderation.service.ModerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Moderator-only queue.
 *
 * The extra @PreAuthorize mirrors the /api/admin/** rule in SecurityConfig so a
 * future route change cannot silently expose the queue.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminModerationController {

    private final ModerationService moderationService;

    public AdminModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> listReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Reports fetched",
                moderationService.listReports(status, targetType, page, size)));
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Report fetched", moderationService.getReport(id)));
    }

    @PutMapping("/reports/{id}/claim")
    public ResponseEntity<ApiResponse<ReportResponse>> claim(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Report claimed",
                moderationService.claimReport(principal.getUsername(), id)));
    }

    @PutMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<ReportResponse>> resolve(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody ResolveReportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Report resolved",
                moderationService.resolveReport(principal.getUsername(), id, request)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ModerationStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Moderation stats fetched", moderationService.stats()));
    }

    @PutMapping("/problems/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreProblem(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody(required = false) RestoreContentRequest request) {
        moderationService.restoreProblem(
                principal.getUsername(), id, request != null ? request.getNote() : null);
        return ResponseEntity.ok(ApiResponse.ok("Question restored", null));
    }

    @PutMapping("/answers/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreAnswer(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody(required = false) RestoreContentRequest request) {
        moderationService.restoreAnswer(
                principal.getUsername(), id, request != null ? request.getNote() : null);
        return ResponseEntity.ok(ApiResponse.ok("Answer restored", null));
    }

    @PutMapping("/users/{id}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @RequestBody(required = false) RestoreContentRequest request) {
        moderationService.reactivateUser(
                principal.getUsername(), id, request != null ? request.getNote() : null);
        return ResponseEntity.ok(ApiResponse.ok("Account reactivated", null));
    }
}
