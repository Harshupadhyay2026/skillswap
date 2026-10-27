package com.skillswap.modules.notification.entity;

/** What happened — used by the frontend to pick an icon and grouping. */
public enum NotificationType {
    SWAP_PROPOSED,
    SWAP_ACCEPTED,
    SWAP_DECLINED,
    SWAP_COMPLETED,
    SWAP_CANCELLED,
    LEARNING_REQUESTED,
    LEARNING_ACCEPTED,
    LEARNING_DECLINED,
    LEARNING_COMPLETED,
    LEARNING_CANCELLED,
    ANSWER_ACCEPTED,
    REPORT_RECEIVED,
    CONTENT_HIDDEN,
    CONTENT_RESTORED,
    ACCOUNT_SUSPENDED,
    ACCOUNT_REACTIVATED
}
