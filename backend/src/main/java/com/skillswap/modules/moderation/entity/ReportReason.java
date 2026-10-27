package com.skillswap.modules.moderation.entity;

/** Why something was reported. The priority drives the admin queue ordering. */
public enum ReportReason {
    SPAM,
    HARASSMENT,
    HATE,
    MISINFORMATION,
    IMPERSONATION,
    OTHER;

    public ReportPriority priority() {
        return switch (this) {
            case HARASSMENT, HATE, IMPERSONATION -> ReportPriority.HIGH;
            case SPAM -> ReportPriority.LOW;
            default -> ReportPriority.NORMAL;
        };
    }
}
