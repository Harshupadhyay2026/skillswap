package com.skillswap.modules.skill.entity;

import com.skillswap.modules.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_skills",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill_id", "skill_type"}))
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    // "TEACH" or "LEARN"
    @Column(name = "skill_type", nullable = false, length = 10)
    private String skillType;

    // "BEGINNER", "INTERMEDIATE", "ADVANCED"
    @Column(nullable = false, length = 20)
    private String proficiency;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UserSkill() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public String getSkillType() { return skillType; }
    public void setSkillType(String skillType) { this.skillType = skillType; }

    public String getProficiency() { return proficiency; }
    public void setProficiency(String proficiency) { this.proficiency = proficiency; }

    public Instant getCreatedAt() { return createdAt; }
}
