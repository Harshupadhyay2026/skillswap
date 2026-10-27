package com.skillswap.modules.problem.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.common.PageResponse;
import com.skillswap.modules.problem.dto.AnswerResponse;
import com.skillswap.modules.problem.dto.CreateAnswerRequest;
import com.skillswap.modules.problem.dto.CreateProblemRequest;
import com.skillswap.modules.problem.dto.ProblemResponse;
import com.skillswap.modules.problem.dto.ProblemSummaryResponse;
import com.skillswap.modules.problem.service.ProblemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public Q&A endpoints.
 *
 * Reads are public (browsing questions should never require an account);
 * writes require authentication and are enforced in SecurityConfig.
 */
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    /** GET /api/problems?query=&category=&status=&page=&size= */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProblemSummaryResponse>>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Problems fetched",
                problemService.listProblems(query, category, status, page, size)));
    }

    /** GET /api/problems/mine — questions I asked. */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ProblemSummaryResponse>>> mine(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Your questions fetched",
                problemService.getMyProblems(principal.getUsername())));
    }

    /** GET /api/problems/{id} — detail; increments the view count for visible questions. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProblemResponse>> detail(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Problem fetched",
                problemService.getProblem(id, principal != null ? principal.getUsername() : null)));
    }

    /** POST /api/problems — ask a question. */
    @PostMapping
    public ResponseEntity<ApiResponse<ProblemResponse>> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateProblemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Question posted!",
                problemService.createProblem(principal.getUsername(), req)));
    }

    /** PUT /api/problems/{id}/close — author closes without accepting. */
    @PutMapping("/{id}/close")
    public ResponseEntity<ApiResponse<ProblemResponse>> close(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Question closed.", problemService.closeProblem(principal.getUsername(), id)));
    }

    /** PUT /api/problems/{id}/reopen */
    @PutMapping("/{id}/reopen")
    public ResponseEntity<ApiResponse<ProblemResponse>> reopen(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Question reopened.", problemService.reopenProblem(principal.getUsername(), id)));
    }

    /** POST /api/problems/{id}/answers — answer a question. */
    @PostMapping("/{id}/answers")
    public ResponseEntity<ApiResponse<AnswerResponse>> answer(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody CreateAnswerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Answer posted!",
                problemService.addAnswer(principal.getUsername(), id, req)));
    }

    /** PUT /api/problems/{id}/answers/{answerId}/accept — author only. */
    @PutMapping("/{id}/answers/{answerId}/accept")
    public ResponseEntity<ApiResponse<ProblemResponse>> accept(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id,
            @PathVariable Long answerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Answer accepted — the question is now solved.",
                problemService.acceptAnswer(principal.getUsername(), id, answerId)));
    }
}
