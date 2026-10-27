package com.skillswap.modules.review.entity;

import com.skillswap.modules.learning.entity.LearningRequest;
import com.skillswap.modules.swap.entity.SkillSwapRequest;
import com.skillswap.modules.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * A rating left by one participant of a completed swap or learning session
 * about the other participant.
 *
 * Exactly one of {@code swap} / {@code learningRequest} is set, so a review is
 * always anchored to a real, completed interaction — there is no way to rate
 * someone you never actually met.
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_swap", columnNames = {"reviewer_id", "swap_id"}),
        @UniqueConstraint(name = "uk_review_learning", columnNames = {"reviewer_id", "learning_request_id"})
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "swap_id")
    private SkillSwapRequest swap;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "learning_request_id")
    private LearningRequest learningRequest;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Review() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    /** "SWAP" or "LEARNING" — what this review is about. */
    @Transient
    public String getContextType() {
        return swap != null ? "SWAP" : "LEARNING";
    }

    /** The skill involved, used for display without extra lookups. */
    @Transient
    public String getSkillName() {
        if (swap != null) return swap.getRequestedSkill() != null
                ? swap.getRequestedSkill().getName() : null;
        if (learningRequest != null && learningRequest.getSkill() != null) {
            return learningRequest.getSkill().getName();
        }
        return null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }

    public User getReviewee() { return reviewee; }
    public void setReviewee(User reviewee) { this.reviewee = reviewee; }

    public SkillSwapRequest getSwap() { return swap; }
    public void setSwap(SkillSwapRequest swap) { this.swap = swap; }

    public LearningRequest getLearningRequest() { return learningRequest; }
    public void setLearningRequest(LearningRequest learningRequest) { this.learningRequest = learningRequest; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
