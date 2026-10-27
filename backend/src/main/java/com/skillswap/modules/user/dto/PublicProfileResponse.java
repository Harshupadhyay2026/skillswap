package com.skillswap.modules.user.dto;

import com.skillswap.modules.skill.dto.UserSkillResponse;

import java.util.List;

/** Public profile: safe user fields plus the skills they teach and want to learn. */
public class PublicProfileResponse {

    private PublicUserResponse user;
    private List<UserSkillResponse> skills;
    private long teachingCount;
    private long learningCount;
    private Double avgRating;
    private long reviewCount;
    private int helpfulPoints;
    private long questionsAsked;
    private long answersGiven;
    private long acceptedAnswers;

    public PublicProfileResponse() {}

    public PublicUserResponse getUser() { return user; }
    public void setUser(PublicUserResponse user) { this.user = user; }

    public List<UserSkillResponse> getSkills() { return skills; }
    public void setSkills(List<UserSkillResponse> skills) { this.skills = skills; }

    public long getTeachingCount() { return teachingCount; }
    public void setTeachingCount(long teachingCount) { this.teachingCount = teachingCount; }

    public long getLearningCount() { return learningCount; }
    public void setLearningCount(long learningCount) { this.learningCount = learningCount; }

    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }

    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }

    public int getHelpfulPoints() { return helpfulPoints; }
    public void setHelpfulPoints(int helpfulPoints) { this.helpfulPoints = helpfulPoints; }

    public long getQuestionsAsked() { return questionsAsked; }
    public void setQuestionsAsked(long questionsAsked) { this.questionsAsked = questionsAsked; }

    public long getAnswersGiven() { return answersGiven; }
    public void setAnswersGiven(long answersGiven) { this.answersGiven = answersGiven; }

    public long getAcceptedAnswers() { return acceptedAnswers; }
    public void setAcceptedAnswers(long acceptedAnswers) { this.acceptedAnswers = acceptedAnswers; }
}
