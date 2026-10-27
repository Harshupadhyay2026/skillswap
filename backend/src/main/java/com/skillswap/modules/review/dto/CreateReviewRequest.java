package com.skillswap.modules.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create a review. Exactly one of {@code swapId} / {@code learningRequestId}
 * must be supplied — the service enforces it.
 */
public class CreateReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;

    private Long swapId;

    private Long learningRequestId;

    public CreateReviewRequest() {}

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Long getSwapId() { return swapId; }
    public void setSwapId(Long swapId) { this.swapId = swapId; }

    public Long getLearningRequestId() { return learningRequestId; }
    public void setLearningRequestId(Long learningRequestId) { this.learningRequestId = learningRequestId; }
}
