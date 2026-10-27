package com.skillswap.modules.chat.entity;

import com.skillswap.modules.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/** One message inside a {@link Conversation}. */
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_conversation", columnList = "conversation_id,id")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String body;

    /** Null until the recipient opens the thread. */
    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Message() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isRead() { return readAt != null; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
