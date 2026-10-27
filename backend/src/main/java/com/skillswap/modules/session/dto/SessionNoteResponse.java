package com.skillswap.modules.session.dto;

import com.skillswap.modules.session.entity.SessionNote;
import com.skillswap.modules.session.entity.SessionType;
import com.skillswap.modules.user.dto.PublicUserResponse;

import java.time.Instant;

public class SessionNoteResponse {

    private Long id;
    private SessionType sessionType;
    private Long sessionId;
    private String title;
    private String content;
    private String nextStep;
    private PublicUserResponse author;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean edited;

    public SessionNoteResponse() {}

    public static SessionNoteResponse fromEntity(SessionNote note) {
        SessionNoteResponse res = new SessionNoteResponse();
        res.setId(note.getId());
        res.setSessionType(note.getSessionType());
        res.setSessionId(note.getSessionId());
        res.setTitle(note.getTitle());
        res.setContent(note.getContent());
        res.setNextStep(note.getNextStep());
        res.setAuthor(PublicUserResponse.fromEntity(note.getAuthor()));
        res.setCreatedAt(note.getCreatedAt());
        res.setUpdatedAt(note.getUpdatedAt());
        res.setEdited(note.getCreatedAt() != null
                && note.getUpdatedAt() != null
                && note.getUpdatedAt().isAfter(note.getCreatedAt()));
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SessionType getSessionType() { return sessionType; }
    public void setSessionType(SessionType sessionType) { this.sessionType = sessionType; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getNextStep() { return nextStep; }
    public void setNextStep(String nextStep) { this.nextStep = nextStep; }

    public PublicUserResponse getAuthor() { return author; }
    public void setAuthor(PublicUserResponse author) { this.author = author; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }
}
