package com.skillswap.modules.moderation.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Append-only record of what an admin did, so every decision is reversible and reviewable. */
@Entity
@Table(name = "moderation_actions", indexes = {
        @Index(name = "idx_moderation_report", columnList = "report_id"),
        @Index(name = "idx_moderation_target", columnList = "target_type,target_id"),
        @Index(name = "idx_moderation_created", columnList = "created_at")
})
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for direct restores/reactivations that are not tied to a report. */
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModerationActionType action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public ModerationAction() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public ModerationActionType getAction() { return action; }
    public void setAction(ModerationActionType action) { this.action = action; }

    public ReportTargetType getTargetType() { return targetType; }
    public void setTargetType(ReportTargetType targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
