package com.skillswap.modules.progress.dto;

import com.skillswap.modules.session.entity.SessionType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One skill's learning progress, derived from completed swap/learning
 * sessions. This is a read model — no progress state is stored separately,
 * so the numbers always match the session history.
 */
public class SkillProgressResponse {

    private Long skillId;
    private String skillName;
    private String skillIcon;
    private String skillCategory;
    private int completedSessions;
    private int targetSessions;
    private int progressPercent;
    private String stage;
    private Instant lastSessionAt;
    private SessionType latestSessionType;
    private Long latestSessionId;
    private List<String> teachers = new ArrayList<>();

    public SkillProgressResponse() {}

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getSkillIcon() { return skillIcon; }
    public void setSkillIcon(String skillIcon) { this.skillIcon = skillIcon; }

    public String getSkillCategory() { return skillCategory; }
    public void setSkillCategory(String skillCategory) { this.skillCategory = skillCategory; }

    public int getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(int completedSessions) { this.completedSessions = completedSessions; }

    public int getTargetSessions() { return targetSessions; }
    public void setTargetSessions(int targetSessions) { this.targetSessions = targetSessions; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public Instant getLastSessionAt() { return lastSessionAt; }
    public void setLastSessionAt(Instant lastSessionAt) { this.lastSessionAt = lastSessionAt; }

    public SessionType getLatestSessionType() { return latestSessionType; }
    public void setLatestSessionType(SessionType latestSessionType) { this.latestSessionType = latestSessionType; }

    public Long getLatestSessionId() { return latestSessionId; }
    public void setLatestSessionId(Long latestSessionId) { this.latestSessionId = latestSessionId; }

    public List<String> getTeachers() { return teachers; }
    public void setTeachers(List<String> teachers) { this.teachers = teachers; }
}
