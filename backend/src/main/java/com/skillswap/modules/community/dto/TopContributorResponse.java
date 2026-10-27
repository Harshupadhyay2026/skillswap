package com.skillswap.modules.community.dto;

/** One row on the public top-contributors leaderboard. */
public class TopContributorResponse {

    private int rank;
    private Long userId;
    private String fullName;
    private String username;
    private String avatarUrl;
    private String college;
    private String branch;
    private String yearOfStudy;
    private int helpfulPoints;
    private long acceptedAnswers;
    private long answersGiven;
    private long questionsAsked;
    private Double avgRating;
    private long reviewCount;

    public TopContributorResponse() {}

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public int getHelpfulPoints() { return helpfulPoints; }
    public void setHelpfulPoints(int helpfulPoints) { this.helpfulPoints = helpfulPoints; }

    public long getAcceptedAnswers() { return acceptedAnswers; }
    public void setAcceptedAnswers(long acceptedAnswers) { this.acceptedAnswers = acceptedAnswers; }

    public long getAnswersGiven() { return answersGiven; }
    public void setAnswersGiven(long answersGiven) { this.answersGiven = answersGiven; }

    public long getQuestionsAsked() { return questionsAsked; }
    public void setQuestionsAsked(long questionsAsked) { this.questionsAsked = questionsAsked; }

    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }

    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
}
