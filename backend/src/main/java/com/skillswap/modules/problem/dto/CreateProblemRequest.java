package com.skillswap.modules.problem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateProblemRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 10000, message = "Description is too long")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 80, message = "Category cannot exceed 80 characters")
    private String category;

    @Size(max = 20, message = "Level cannot exceed 20 characters")
    private String level;

    /** Optional skill catalog link, used for the icon. */
    private Long skillId;

    public CreateProblemRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
}
