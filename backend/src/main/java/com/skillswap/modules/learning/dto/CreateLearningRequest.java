package com.skillswap.modules.learning.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class CreateLearningRequest {

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;

    private Instant scheduledAt;

    public CreateLearningRequest() {}

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
}
