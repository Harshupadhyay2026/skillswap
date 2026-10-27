package com.skillswap.modules.chat.dto;

import com.skillswap.modules.user.dto.PublicUserResponse;

import java.time.Instant;

public class ConversationResponse {

    private Long id;
    private PublicUserResponse counterpart;
    private String lastMessage;
    private Long lastMessageSenderId;
    private Instant lastMessageAt;
    private long unreadCount;
    private Instant createdAt;

    public ConversationResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PublicUserResponse getCounterpart() { return counterpart; }
    public void setCounterpart(PublicUserResponse counterpart) { this.counterpart = counterpart; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Long getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(Long lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
