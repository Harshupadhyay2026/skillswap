package com.skillswap.modules.learning.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.learning.dto.CreateLearningRequest;
import com.skillswap.modules.learning.dto.LearningRequestResponse;
import com.skillswap.modules.learning.service.LearningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

        /** POST /api/learning — request a free shared session for a skill. */
    @PostMapping
    public ResponseEntity<ApiResponse<LearningRequestResponse>> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateLearningRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Learning request sent",
                learningService.createRequest(principal.getUsername(), req)));
    }

    /** GET /api/learning/incoming — requests where I am the teacher. */
    @GetMapping("/incoming")
    public ResponseEntity<ApiResponse<List<LearningRequestResponse>>> incoming(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Incoming learning requests fetched",
                learningService.getIncoming(principal.getUsername())));
    }

    /** GET /api/learning/outgoing — requests where I am the learner. */
    @GetMapping("/outgoing")
    public ResponseEntity<ApiResponse<List<LearningRequestResponse>>> outgoing(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Outgoing learning requests fetched",
                learningService.getOutgoing(principal.getUsername())));
    }

    /** GET /api/learning/all — both directions. */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<LearningRequestResponse>>> all(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Learning requests fetched",
                learningService.getAllMine(principal.getUsername())));
    }

    /** GET /api/learning/{id} — one session, participants only (used by the video room page). */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LearningRequestResponse>> getOne(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Learning request fetched",
                learningService.getMine(principal.getUsername(), id)));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<LearningRequestResponse>> accept(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Learning request accepted! Meeting room created.",
                learningService.accept(principal.getUsername(), id)));
    }

    @PutMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<LearningRequestResponse>> decline(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Learning request declined.",
                learningService.decline(principal.getUsername(), id)));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<LearningRequestResponse>> complete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Learning session marked as completed!",
                learningService.complete(principal.getUsername(), id)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<LearningRequestResponse>> cancel(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Learning request cancelled.",
                learningService.cancel(principal.getUsername(), id)));
    }
}
