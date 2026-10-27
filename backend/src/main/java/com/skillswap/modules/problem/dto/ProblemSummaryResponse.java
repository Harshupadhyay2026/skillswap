package com.skillswap.modules.problem.dto;

import com.skillswap.modules.problem.entity.Problem;
import com.skillswap.modules.user.dto.PublicUserResponse;

import java.time.Instant;

/** List-view problem: no answers, just the summary fields. */
public class ProblemSummaryResponse {

    private Long id;
    private PublicUserResponse author;
    private String title;
    private String description;
    private String category;
    private String level;
    private String skillIcon;
    private String status;
    private long views;
    private long answerCount;
    private boolean hidden;
    private Instant createdAt;

    public ProblemSummaryResponse() {}

    public static ProblemSummaryResponse fromEntity(Problem p, long answerCount) {
        ProblemSummaryResponse res = new ProblemSummaryResponse();
        res.setId(p.getId());
        res.setAuthor(PublicUserResponse.fromEntity(p.getAuthor()));
        res.setTitle(p.getTitle());
        res.setDescription(p.getDescription());
        res.setCategory(p.getCategory());
        res.setLevel(p.getLevel());
        res.setSkillIcon(p.getSkill() != null ? p.getSkill().getIcon() : null);
        res.setStatus(p.getStatus().name());
        res.setViews(p.getViews());
        res.setAnswerCount(answerCount);
        res.setHidden(p.isHidden());
        res.setCreatedAt(p.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PublicUserResponse getAuthor() { return author; }
    public void setAuthor(PublicUserResponse author) { this.author = author; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSkillIcon() { return skillIcon; }
    public void setSkillIcon(String skillIcon) { this.skillIcon = skillIcon; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }

    public long getAnswerCount() { return answerCount; }
    public void setAnswerCount(long answerCount) { this.answerCount = answerCount; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
