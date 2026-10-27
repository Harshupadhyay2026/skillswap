package com.skillswap.modules.problem.dto;

import com.skillswap.modules.problem.entity.Answer;
import com.skillswap.modules.user.dto.PublicUserResponse;

import java.time.Instant;

public class AnswerResponse {

    private Long id;
    private Long problemId;
    private PublicUserResponse author;
    private String body;
    private boolean accepted;
    private Instant createdAt;

    public AnswerResponse() {}

    public static AnswerResponse fromEntity(Answer a) {
        AnswerResponse res = new AnswerResponse();
        res.setId(a.getId());
        res.setProblemId(a.getProblem() != null ? a.getProblem().getId() : null);
        res.setAuthor(PublicUserResponse.fromEntity(a.getAuthor()));
        res.setBody(a.getBody());
        res.setAccepted(a.isAccepted());
        res.setCreatedAt(a.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }

    public PublicUserResponse getAuthor() { return author; }
    public void setAuthor(PublicUserResponse author) { this.author = author; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
