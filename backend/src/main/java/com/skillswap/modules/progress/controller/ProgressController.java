package com.skillswap.modules.progress.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.progress.dto.ProgressResponse;
import com.skillswap.modules.progress.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /** GET /api/progress/me — the logged-in user's learning summary. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProgressResponse>> myProgress(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Learning progress fetched",
                progressService.getMyProgress(principal.getUsername())));
    }
}
