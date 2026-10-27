package com.skillswap.modules.review.dto;

import java.time.Instant;

/**
 * A completed swap or session the current user has not reviewed yet.
 * Drives the "Rate this session" prompt in the History tab.
 */
public class PendingReviewItem {

    private String type;        // "SWAP" | "LEARNING"
    private Long contextId;     // swapId or learningRequestId
    private Long counterpartId;
    private String counterpartName;
    private String skillName;
    private Instant completedAt;

    public PendingReviewItem() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getContextId() { return contextId; }
    public void setContextId(Long contextId) { this.contextId = contextId; }

    public Long getCounterpartId() { return counterpartId; }
    public void setCounterpartId(Long counterpartId) { this.counterpartId = counterpartId; }

    public String getCounterpartName() { return counterpartName; }
    public void setCounterpartName(String counterpartName) { this.counterpartName = counterpartName; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
