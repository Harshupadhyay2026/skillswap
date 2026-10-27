package com.skillswap.modules.moderation.dto;

import com.skillswap.modules.moderation.entity.ModerationActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResolveReportRequest {

    @NotNull(message = "Choose a moderation action")
    private ModerationActionType action;

    @Size(max = 1000, message = "Note cannot exceed 1000 characters")
    private String note;

    public ModerationActionType getAction() { return action; }
    public void setAction(ModerationActionType action) { this.action = action; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
