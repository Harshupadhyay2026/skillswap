package com.skillswap.modules.moderation.dto;

import com.skillswap.modules.moderation.entity.ModerationAction;
import com.skillswap.modules.moderation.entity.ModerationActionType;

import java.time.Instant;

public class ModerationActionResponse {

    private Long id;
    private ModerationActionType action;
    private Long adminId;
    private String adminName;
    private String note;
    private Instant createdAt;

    public static ModerationActionResponse fromEntity(ModerationAction action, String adminName) {
        ModerationActionResponse res = new ModerationActionResponse();
        res.setId(action.getId());
        res.setAction(action.getAction());
        res.setAdminId(action.getAdminId());
        res.setAdminName(adminName);
        res.setNote(action.getNote());
        res.setCreatedAt(action.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ModerationActionType getAction() { return action; }
    public void setAction(ModerationActionType action) { this.action = action; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
