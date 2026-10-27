package com.skillswap.modules.moderation.dto;

public class ModerationStatsResponse {

    private long openReports;
    private long inReviewReports;
    private long resolvedToday;
    private long suspendedUsers;

    public long getOpenReports() { return openReports; }
    public void setOpenReports(long openReports) { this.openReports = openReports; }

    public long getInReviewReports() { return inReviewReports; }
    public void setInReviewReports(long inReviewReports) { this.inReviewReports = inReviewReports; }

    public long getResolvedToday() { return resolvedToday; }
    public void setResolvedToday(long resolvedToday) { this.resolvedToday = resolvedToday; }

    public long getSuspendedUsers() { return suspendedUsers; }
    public void setSuspendedUsers(long suspendedUsers) { this.suspendedUsers = suspendedUsers; }
}
