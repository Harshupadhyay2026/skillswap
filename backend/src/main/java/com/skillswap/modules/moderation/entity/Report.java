package com.skillswap.modules.moderation.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A moderation report.
 *
 * User references are stored as IDs rather than JPA relationships: reports are
 * an audit trail and must survive account changes without cascade surprises.
 */
@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_status_created", columnList = "status,created_at"),
        @Index(name = "idx_report_target", columnList = "target_type,target_id"),
        @Index(name = "idx_report_reporter_created", columnList = "reporter_id,created_at")
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /** Author/owner of the reported content, captured when the report is filed. */
    @Column(name = "target_owner_id")
    private Long targetOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportPriority priority = ReportPriority.NORMAL;

    @Column(name = "reviewed_by_id")
    private Long reviewedById;

    private Instant reviewedAt;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Report() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long reporterId) { this.reporterId = reporterId; }

    public ReportTargetType getTargetType() { return targetType; }
    public void setTargetType(ReportTargetType targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public Long getTargetOwnerId() { return targetOwnerId; }
    public void setTargetOwnerId(Long targetOwnerId) { this.targetOwnerId = targetOwnerId; }

    public ReportReason getReason() { return reason; }
    public void setReason(ReportReason reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public ReportPriority getPriority() { return priority; }
    public void setPriority(ReportPriority priority) { this.priority = priority; }

    public Long getReviewedById() { return reviewedById; }
    public void setReviewedById(Long reviewedById) { this.reviewedById = reviewedById; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
