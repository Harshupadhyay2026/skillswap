package com.skillswap.modules.swap.dto;

import com.skillswap.modules.skill.dto.SkillResponse;
import com.skillswap.modules.swap.entity.SkillSwapRequest;
import com.skillswap.modules.swap.entity.SwapStatus;
import com.skillswap.modules.user.dto.PublicUserResponse;

import java.time.Instant;

public class SwapResponse {

    private Long id;
    private PublicUserResponse sender;
    private PublicUserResponse receiver;
    private SkillResponse offeredSkill;
    private SkillResponse requestedSkill;
    private SwapStatus status;
    private String message;
    private Instant scheduledAt;
    private String meetingLink;
    private Instant createdAt;
    private Instant updatedAt;

    public SwapResponse() {}

    public static SwapResponse fromEntity(SkillSwapRequest swap) {
        SwapResponse res = new SwapResponse();
        res.setId(swap.getId());
        res.setSender(PublicUserResponse.fromEntity(swap.getSender()));
        res.setReceiver(PublicUserResponse.fromEntity(swap.getReceiver()));
        res.setOfferedSkill(SkillResponse.fromEntity(swap.getOfferedSkill()));
        res.setRequestedSkill(SkillResponse.fromEntity(swap.getRequestedSkill()));
        res.setStatus(swap.getStatus());
        res.setMessage(swap.getMessage());
        res.setScheduledAt(swap.getScheduledAt());
        res.setMeetingLink(swap.getMeetingLink());
        res.setCreatedAt(swap.getCreatedAt());
        res.setUpdatedAt(swap.getUpdatedAt());
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PublicUserResponse getSender() {
        return sender;
    }

    public void setSender(PublicUserResponse sender) {
        this.sender = sender;
    }

    public PublicUserResponse getReceiver() {
        return receiver;
    }

    public void setReceiver(PublicUserResponse receiver) {
        this.receiver = receiver;
    }

    public SkillResponse getOfferedSkill() {
        return offeredSkill;
    }

    public void setOfferedSkill(SkillResponse offeredSkill) {
        this.offeredSkill = offeredSkill;
    }

    public SkillResponse getRequestedSkill() {
        return requestedSkill;
    }

    public void setRequestedSkill(SkillResponse requestedSkill) {
        this.requestedSkill = requestedSkill;
    }

    public SwapStatus getStatus() {
        return status;
    }

    public void setStatus(SwapStatus status) {
        this.status = status;
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

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
