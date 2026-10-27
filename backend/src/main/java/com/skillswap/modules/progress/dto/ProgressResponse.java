package com.skillswap.modules.progress.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Dashboard summary plus one row per skill the user is learning. */
public class ProgressResponse {

    private int learningSessions;
    private int teachingSessions;
    private int skillsInProgress;
    private long notesWritten;
    private Instant lastActivityAt;
    private List<SkillProgressResponse> skills = new ArrayList<>();

    public ProgressResponse() {}

    public int getLearningSessions() { return learningSessions; }
    public void setLearningSessions(int learningSessions) { this.learningSessions = learningSessions; }

    public int getTeachingSessions() { return teachingSessions; }
    public void setTeachingSessions(int teachingSessions) { this.teachingSessions = teachingSessions; }

    public int getSkillsInProgress() { return skillsInProgress; }
    public void setSkillsInProgress(int skillsInProgress) { this.skillsInProgress = skillsInProgress; }

    public long getNotesWritten() { return notesWritten; }
    public void setNotesWritten(long notesWritten) { this.notesWritten = notesWritten; }

    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public List<SkillProgressResponse> getSkills() { return skills; }
    public void setSkills(List<SkillProgressResponse> skills) { this.skills = skills; }
}
