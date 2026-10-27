package com.skillswap.modules.review.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.review.dto.CreateReviewRequest;
import com.skillswap.modules.review.dto.PendingReviewItem;
import com.skillswap.modules.review.dto.ReviewResponse;
import com.skillswap.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** POST /api/reviews — rate a completed swap or session. */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateReviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Review submitted. Thanks for helping the community!",
                reviewService.createReview(principal.getUsername(), req)));
    }

    /** GET /api/reviews/pending — completed interactions awaiting my review. */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PendingReviewItem>>> pending(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Pending reviews fetched",
                reviewService.getPendingReviews(principal.getUsername())));
    }

    /** GET /api/reviews/user/{userId} — public reviews about a user. */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> forUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Reviews fetched",
                reviewService.getReviewsForUser(userId)));
    }
}
