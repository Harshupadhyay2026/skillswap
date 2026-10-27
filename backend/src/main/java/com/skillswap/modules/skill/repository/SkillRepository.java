package com.skillswap.modules.skill.repository;

import com.skillswap.modules.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNameIgnoreCase(String name);

    // Used by service to search by name — case insensitive
    List<Skill> findByNameContainingIgnoreCase(String name);

    List<Skill> findByCategoryIgnoreCase(String category);

    List<Skill> findByNameContainingIgnoreCaseAndCategoryIgnoreCase(String name, String category);
}
