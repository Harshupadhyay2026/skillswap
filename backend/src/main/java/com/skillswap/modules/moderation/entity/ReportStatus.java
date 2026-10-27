package com.skillswap.modules.moderation.entity;

public enum ReportStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED,
    DISMISSED;

    public boolean isOpen() {
        return this == OPEN || this == IN_REVIEW;
    }
}
