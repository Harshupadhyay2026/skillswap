package com.skillswap.modules.learning.dto;

import com.skillswap.modules.learning.entity.LearningRequest;
import com.skillswap.modules.learning.entity.LearningStatus;
import com.skillswap.modules.skill.dto.SkillResponse;
import com.skillswap.modules.user.dto.PublicUserResponse;

import java.time.Instant;

public class LearningRequestResponse {

    private Long id;
    private PublicUserResponse learner;
    private PublicUserResponse teacher;
    private SkillResponse skill;
    private LearningStatus status;
    private String message;
    private Instant scheduledAt;
    private String meetingLink;
    private Instant createdAt;
    private Instant updatedAt;

    public LearningRequestResponse() {}

    public static LearningRequestResponse fromEntity(LearningRequest req) {
        LearningRequestResponse res = new LearningRequestResponse();
        res.setId(req.getId());
        res.setLearner(PublicUserResponse.fromEntity(req.getLearner()));
        res.setTeacher(PublicUserResponse.fromEntity(req.getTeacher()));
        res.setSkill(SkillResponse.fromEntity(req.getSkill()));
        res.setStatus(req.getStatus());
        res.setMessage(req.getMessage());
        res.setScheduledAt(req.getScheduledAt());
        res.setMeetingLink(req.getMeetingLink());
        res.setCreatedAt(req.getCreatedAt());
        res.setUpdatedAt(req.getUpdatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PublicUserResponse getLearner() { return learner; }
    public void setLearner(PublicUserResponse learner) { this.learner = learner; }

    public PublicUserResponse getTeacher() { return teacher; }
    public void setTeacher(PublicUserResponse teacher) { this.teacher = teacher; }

    public SkillResponse getSkill() { return skill; }
    public void setSkill(SkillResponse skill) { this.skill = skill; }

    public LearningStatus getStatus() { return status; }
    public void setStatus(LearningStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
