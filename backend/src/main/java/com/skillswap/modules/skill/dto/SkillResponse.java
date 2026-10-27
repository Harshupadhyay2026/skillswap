package com.skillswap.modules.skill.dto;

public class SkillResponse {
    private Long id;
    private String name;
    private String category;
    private String icon;

    public SkillResponse() {}

    public SkillResponse(Long id, String name, String category, String icon) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.icon = icon;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public static SkillResponse fromEntity(com.skillswap.modules.skill.entity.Skill skill) {
        if (skill == null) return null;
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory(), skill.getIcon());
    }
}
