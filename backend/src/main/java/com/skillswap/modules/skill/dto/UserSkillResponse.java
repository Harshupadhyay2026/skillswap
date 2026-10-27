package com.skillswap.modules.skill.dto;

import java.time.Instant;

public class UserSkillResponse {
    private Long id;
    private Long skillId;
    private String skillName;
    private String skillCategory;
    private String skillIcon;
    private String skillType;       // "TEACH" or "LEARN"
    private String proficiency;
    private Long teacherId;
    private String teacherName;
    private String teacherCollege;
    private String teacherBranch;
    private String teacherYear;
    private String teacherCity;
    private String teacherBio;
    private String teacherAvatarUrl;
    private Integer teacherHelpfulPoints;
    private String teacherLearningMode;
    private Instant teacherJoinedAt;
    private Double avgRating;
    private Long reviewCount;

    public UserSkillResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getSkillCategory() { return skillCategory; }
    public void setSkillCategory(String skillCategory) { this.skillCategory = skillCategory; }

    public String getSkillIcon() { return skillIcon; }
    public void setSkillIcon(String skillIcon) { this.skillIcon = skillIcon; }

    public String getSkillType() { return skillType; }
    public void setSkillType(String skillType) { this.skillType = skillType; }

    public String getProficiency() { return proficiency; }
    public void setProficiency(String proficiency) { this.proficiency = proficiency; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getTeacherCollege() { return teacherCollege; }
    public void setTeacherCollege(String teacherCollege) { this.teacherCollege = teacherCollege; }

    public String getTeacherBranch() { return teacherBranch; }
    public void setTeacherBranch(String teacherBranch) { this.teacherBranch = teacherBranch; }

    public String getTeacherYear() { return teacherYear; }
    public void setTeacherYear(String teacherYear) { this.teacherYear = teacherYear; }

    public String getTeacherCity() { return teacherCity; }
    public void setTeacherCity(String teacherCity) { this.teacherCity = teacherCity; }

    public String getTeacherBio() { return teacherBio; }
    public void setTeacherBio(String teacherBio) { this.teacherBio = teacherBio; }

    public String getTeacherAvatarUrl() { return teacherAvatarUrl; }
    public void setTeacherAvatarUrl(String teacherAvatarUrl) { this.teacherAvatarUrl = teacherAvatarUrl; }

    public Integer getTeacherHelpfulPoints() { return teacherHelpfulPoints; }
    public void setTeacherHelpfulPoints(Integer teacherHelpfulPoints) { this.teacherHelpfulPoints = teacherHelpfulPoints; }

    public String getTeacherLearningMode() { return teacherLearningMode; }
    public void setTeacherLearningMode(String teacherLearningMode) { this.teacherLearningMode = teacherLearningMode; }

    public Instant getTeacherJoinedAt() { return teacherJoinedAt; }
    public void setTeacherJoinedAt(Instant teacherJoinedAt) { this.teacherJoinedAt = teacherJoinedAt; }

    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }

    public Long getReviewCount() { return reviewCount; }
    public void setReviewCount(Long reviewCount) { this.reviewCount = reviewCount; }
}
