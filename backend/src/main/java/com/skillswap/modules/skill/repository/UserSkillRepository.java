package com.skillswap.modules.skill.repository;

import com.skillswap.modules.skill.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    List<UserSkill> findByUserId(Long userId);

    List<UserSkill> findByUserIdAndSkillType(Long userId, String skillType);

    Optional<UserSkill> findByUserIdAndSkillIdAndSkillType(Long userId, Long skillId, String skillType);

    // All users who teach — filtered post-load in service by query/skillId when needed
    @Query("SELECT us FROM UserSkill us JOIN FETCH us.user u JOIN FETCH us.skill s WHERE us.skillType = 'TEACH'")
    List<UserSkill> findAllTeachers();

    // Teachers filtered by specific skillId
    @Query("SELECT us FROM UserSkill us JOIN FETCH us.user u JOIN FETCH us.skill s WHERE us.skillType = 'TEACH' AND us.skill.id = :skillId")
    List<UserSkill> findTeachersBySkillId(@Param("skillId") Long skillId);

    /**
     * True mutual swap matches: users who teach {@code wanted} AND have registered
     * a LEARN interest in {@code offered}. This is what makes SkillSwap a swap
     * platform rather than a tutoring directory.
     */
    @Query("""
            SELECT us FROM UserSkill us
            JOIN FETCH us.user u
            JOIN FETCH us.skill s
            WHERE us.skillType = 'TEACH'
              AND LOWER(s.name) = LOWER(:wanted)
              AND u.id <> :excludeUserId
              AND EXISTS (
                  SELECT 1 FROM UserSkill w
                  WHERE w.user = u
                    AND w.skillType = 'LEARN'
                    AND LOWER(w.skill.name) = LOWER(:offered)
              )
            """)
    List<UserSkill> findMutualSwapMatches(@Param("offered") String offered,
                                          @Param("wanted") String wanted,
                                          @Param("excludeUserId") Long excludeUserId);

    /** All TEACH/LEARN entries for a user, used by public profiles. */
    @Query("SELECT us FROM UserSkill us JOIN FETCH us.skill s WHERE us.user.id = :userId ORDER BY us.skillType, s.name")
    List<UserSkill> findAllByUserId(@Param("userId") Long userId);

    long countByUserIdAndSkillType(Long userId, String skillType);
}
