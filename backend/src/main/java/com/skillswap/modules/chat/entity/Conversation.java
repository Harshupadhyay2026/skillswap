package com.skillswap.modules.chat.entity;

import com.skillswap.modules.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * A one-to-one conversation.
 *
 * The pair is stored in a canonical order (smaller user id first) and backed by
 * a unique constraint, so "get or create" cannot produce two threads for the
 * same two people under concurrency.
 */
@Entity
@Table(name = "conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversation_pair",
                columnNames = {"user_one_id", "user_two_id"}))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_one_id", nullable = false)
    private User userOne;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_two_id", nullable = false)
    private User userTwo;

    private Instant lastMessageAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Conversation() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    /** The participant who is not {@code userId}. */
    public User counterpartOf(Long userId) {
        return userOne.getId().equals(userId) ? userTwo : userOne;
    }

    public boolean includes(Long userId) {
        return userOne.getId().equals(userId) || userTwo.getId().equals(userId);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUserOne() { return userOne; }
    public void setUserOne(User userOne) { this.userOne = userOne; }

    public User getUserTwo() { return userTwo; }
    public void setUserTwo(User userTwo) { this.userTwo = userTwo; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
