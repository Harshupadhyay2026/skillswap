package com.skillswap.modules.moderation.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.moderation.dto.CreateReportRequest;
import com.skillswap.modules.moderation.dto.ReportCreatedResponse;
import com.skillswap.modules.moderation.service.ModerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Reporting endpoints available to any signed-in member. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ModerationService moderationService;

    public ReportController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReportCreatedResponse>> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateReportRequest request) {
        ReportCreatedResponse created = moderationService.createReport(
                principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Report submitted for review", created));
    }

    /** The reporter's own report history — useful for "already reported" states. */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ReportCreatedResponse>>> mine(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Reports fetched",
                moderationService.myReports(principal.getUsername())));
    }
}
