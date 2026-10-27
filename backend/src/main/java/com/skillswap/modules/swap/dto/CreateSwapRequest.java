package com.skillswap.modules.swap.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class CreateSwapRequest {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    @NotNull(message = "Offered skill ID is required")
    private Long offeredSkillId;

    @NotNull(message = "Requested skill ID is required")
    private Long requestedSkillId;

    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;

    private Instant scheduledAt;

    public CreateSwapRequest() {}

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public Long getOfferedSkillId() {
        return offeredSkillId;
    }

    public void setOfferedSkillId(Long offeredSkillId) {
        this.offeredSkillId = offeredSkillId;
    }

    public Long getRequestedSkillId() {
        return requestedSkillId;
    }

    public void setRequestedSkillId(Long requestedSkillId) {
        this.requestedSkillId = requestedSkillId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
}
