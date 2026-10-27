package com.skillswap.modules.notification.dto;

import com.skillswap.modules.notification.entity.Notification;

import java.time.Instant;

public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String body;
    private String link;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;

    public NotificationResponse() {}

    public static NotificationResponse fromEntity(Notification n) {
        NotificationResponse res = new NotificationResponse();
        res.setId(n.getId());
        res.setType(n.getType().name());
        res.setTitle(n.getTitle());
        res.setBody(n.getBody());
        res.setLink(n.getLink());
        res.setRead(n.isRead());
        res.setReadAt(n.getReadAt());
        res.setCreatedAt(n.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
