package com.skillswap.modules.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class AddUserSkillRequest {

    @NotNull(message = "skillId is required")
    private Long skillId;

    @NotBlank(message = "skillType is required")
    @Pattern(regexp = "^(TEACH|LEARN)$", message = "skillType must be TEACH or LEARN")
    private String skillType;

    @NotBlank(message = "proficiency is required")
    @Pattern(regexp = "^(BEGINNER|INTERMEDIATE|ADVANCED)$",
             message = "proficiency must be BEGINNER, INTERMEDIATE, or ADVANCED")
    private String proficiency;

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    public String getSkillType() { return skillType; }
    public void setSkillType(String skillType) { this.skillType = skillType; }

    public String getProficiency() { return proficiency; }
    public void setProficiency(String proficiency) { this.proficiency = proficiency; }
}
