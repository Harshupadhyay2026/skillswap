package com.skillswap.modules.moderation.dto;

import com.skillswap.modules.moderation.entity.ReportReason;
import com.skillswap.modules.moderation.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateReportRequest {

    @NotNull(message = "Choose what you are reporting")
    private ReportTargetType targetType;

    @NotNull(message = "The reported item is required")
    private Long targetId;

    @NotNull(message = "Choose a reason")
    private ReportReason reason;

    @Size(max = 1000, message = "Details cannot exceed 1000 characters")
    private String details;

    public ReportTargetType getTargetType() { return targetType; }
    public void setTargetType(ReportTargetType targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
