package com.skillswap.modules.moderation.dto;

import jakarta.validation.constraints.Size;

/** Used by the direct restore/reactivate endpoints (not tied to a new report). */
public class RestoreContentRequest {

    @Size(max = 1000, message = "Note cannot exceed 1000 characters")
    private String note;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
