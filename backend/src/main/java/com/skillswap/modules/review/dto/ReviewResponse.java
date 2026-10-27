package com.skillswap.modules.review.dto;

import com.skillswap.modules.review.entity.Review;
import com.skillswap.modules.user.dto.PublicUserResponse;

import java.time.Instant;

public class ReviewResponse {

    private Long id;
    private PublicUserResponse reviewer;
    private Long revieweeId;
    private Integer rating;
    private String comment;
    private String contextType;
    private String skillName;
    private Long swapId;
    private Long learningRequestId;
    private Instant createdAt;

    public ReviewResponse() {}

    public static ReviewResponse fromEntity(Review r) {
        ReviewResponse res = new ReviewResponse();
        res.setId(r.getId());
        res.setReviewer(PublicUserResponse.fromEntity(r.getReviewer()));
        res.setRevieweeId(r.getReviewee() != null ? r.getReviewee().getId() : null);
        res.setRating(r.getRating());
        res.setComment(r.getComment());
        res.setContextType(r.getContextType());
        res.setSkillName(r.getSkillName());
        res.setSwapId(r.getSwap() != null ? r.getSwap().getId() : null);
        res.setLearningRequestId(r.getLearningRequest() != null ? r.getLearningRequest().getId() : null);
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PublicUserResponse getReviewer() { return reviewer; }
    public void setReviewer(PublicUserResponse reviewer) { this.reviewer = reviewer; }

    public Long getRevieweeId() { return revieweeId; }
    public void setRevieweeId(Long revieweeId) { this.revieweeId = revieweeId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getContextType() { return contextType; }
    public void setContextType(String contextType) { this.contextType = contextType; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public Long getSwapId() { return swapId; }
    public void setSwapId(Long swapId) { this.swapId = swapId; }

    public Long getLearningRequestId() { return learningRequestId; }
    public void setLearningRequestId(Long learningRequestId) { this.learningRequestId = learningRequestId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
