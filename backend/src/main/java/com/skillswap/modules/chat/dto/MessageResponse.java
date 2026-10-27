package com.skillswap.modules.chat.dto;

import com.skillswap.modules.chat.entity.Message;
import com.skillswap.modules.user.dto.PublicUserResponse;

import java.time.Instant;

public class MessageResponse {

    private Long id;
    private Long conversationId;
    private PublicUserResponse sender;
    private String body;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;

    public MessageResponse() {}

    public static MessageResponse fromEntity(Message m) {
        MessageResponse res = new MessageResponse();
        res.setId(m.getId());
        res.setConversationId(m.getConversation() != null ? m.getConversation().getId() : null);
        res.setSender(PublicUserResponse.fromEntity(m.getSender()));
        res.setBody(m.getBody());
        res.setRead(m.isRead());
        res.setReadAt(m.getReadAt());
        res.setCreatedAt(m.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public PublicUserResponse getSender() { return sender; }
    public void setSender(PublicUserResponse sender) { this.sender = sender; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
