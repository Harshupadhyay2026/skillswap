package com.skillswap.modules.moderation.dto;

import java.time.Instant;
import java.util.List;

/** Admin queue row/detail. Includes a safe preview of the reported target. */
public class ReportResponse {

    private Long id;
    private String targetType;
    private Long targetId;
    private String targetPreview;
    private String targetContext;
    private boolean targetExists;
    private boolean targetHidden;
    private Long targetOwnerId;
    private String targetOwnerName;
    private String targetOwnerUsername;
    private String targetOwnerAvatarUrl;
    private boolean targetOwnerActive;
    private boolean targetOwnerAdmin;
    private String targetOwnerSuspensionReason;

    private String reason;
    private String priority;
    private String details;
    private String status;

    private Long reporterId;
    private String reporterName;
    private String reporterUsername;
    private long duplicateCount;

    private Instant createdAt;
    private Instant reviewedAt;
    private String reviewedByName;
    private String resolutionNote;
    private List<ModerationActionResponse> actions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getTargetPreview() { return targetPreview; }
    public void setTargetPreview(String targetPreview) { this.targetPreview = targetPreview; }

    public String getTargetContext() { return targetContext; }
    public void setTargetContext(String targetContext) { this.targetContext = targetContext; }

    public boolean isTargetExists() { return targetExists; }
    public void setTargetExists(boolean targetExists) { this.targetExists = targetExists; }

    public boolean isTargetHidden() { return targetHidden; }
    public void setTargetHidden(boolean targetHidden) { this.targetHidden = targetHidden; }

    public Long getTargetOwnerId() { return targetOwnerId; }
    public void setTargetOwnerId(Long targetOwnerId) { this.targetOwnerId = targetOwnerId; }

    public String getTargetOwnerName() { return targetOwnerName; }
    public void setTargetOwnerName(String targetOwnerName) { this.targetOwnerName = targetOwnerName; }

    public String getTargetOwnerUsername() { return targetOwnerUsername; }
    public void setTargetOwnerUsername(String targetOwnerUsername) { this.targetOwnerUsername = targetOwnerUsername; }

    public String getTargetOwnerAvatarUrl() { return targetOwnerAvatarUrl; }
    public void setTargetOwnerAvatarUrl(String targetOwnerAvatarUrl) { this.targetOwnerAvatarUrl = targetOwnerAvatarUrl; }

    public boolean isTargetOwnerActive() { return targetOwnerActive; }
    public void setTargetOwnerActive(boolean targetOwnerActive) { this.targetOwnerActive = targetOwnerActive; }

    public boolean isTargetOwnerAdmin() { return targetOwnerAdmin; }
    public void setTargetOwnerAdmin(boolean targetOwnerAdmin) { this.targetOwnerAdmin = targetOwnerAdmin; }

    public String getTargetOwnerSuspensionReason() { return targetOwnerSuspensionReason; }
    public void setTargetOwnerSuspensionReason(String targetOwnerSuspensionReason) {
        this.targetOwnerSuspensionReason = targetOwnerSuspensionReason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReporterUsername() { return reporterUsername; }
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }

    public long getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(long duplicateCount) { this.duplicateCount = duplicateCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public List<ModerationActionResponse> getActions() { return actions; }
    public void setActions(List<ModerationActionResponse> actions) { this.actions = actions; }
}
