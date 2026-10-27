package com.skillswap.modules.moderation.dto;

import com.skillswap.modules.moderation.entity.Report;
import com.skillswap.modules.moderation.entity.ReportTargetType;

import java.time.Instant;

/** Safe acknowledgement returned to the person who filed the report. */
public class ReportCreatedResponse {

    private Long id;
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
    private String status;
    private Instant createdAt;

    public static ReportCreatedResponse fromEntity(Report report) {
        ReportCreatedResponse res = new ReportCreatedResponse();
        res.setId(report.getId());
        res.setTargetType(report.getTargetType());
        res.setTargetId(report.getTargetId());
        res.setReason(report.getReason().name());
        res.setStatus(report.getStatus().name());
        res.setCreatedAt(report.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReportTargetType getTargetType() { return targetType; }
    public void setTargetType(ReportTargetType targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
