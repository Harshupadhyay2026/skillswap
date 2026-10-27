package com.skillswap.modules.notification.entity;

import com.skillswap.modules.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * An in-app notification for one user.
 *
 * Notifications are written inside the transaction that caused them, so a
 * failed action never leaves a dangling "swap accepted" message.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_notification_user_read", columnList = "user_id,read_at")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 600)
    private String body;

    /** Frontend route to open when the notification is clicked. */
    @Column(length = 200)
    private String link;

    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Notification() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public boolean isRead() { return readAt != null; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
