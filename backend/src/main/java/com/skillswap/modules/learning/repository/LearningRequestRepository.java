package com.skillswap.modules.learning.repository;

import com.skillswap.modules.learning.entity.LearningRequest;
import com.skillswap.modules.learning.entity.LearningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningRequestRepository extends JpaRepository<LearningRequest, Long> {

    @Query("""
            SELECT l FROM LearningRequest l
            WHERE l.teacher.username = :username
            ORDER BY l.createdAt DESC
            """)
    List<LearningRequest> findIncoming(@Param("username") String username);

    @Query("""
            SELECT l FROM LearningRequest l
            WHERE l.learner.username = :username
            ORDER BY l.createdAt DESC
            """)
    List<LearningRequest> findOutgoing(@Param("username") String username);

    @Query("""
            SELECT l FROM LearningRequest l
            WHERE l.learner.username = :username OR l.teacher.username = :username
            ORDER BY l.createdAt DESC
            """)
    List<LearningRequest> findAllMine(@Param("username") String username);

    @Query("""
            SELECT COUNT(l) > 0 FROM LearningRequest l
            WHERE l.learner.id = :learnerId
              AND l.teacher.id = :teacherId
              AND l.skill.id = :skillId
              AND l.status IN (com.skillswap.modules.learning.entity.LearningStatus.PENDING,
                               com.skillswap.modules.learning.entity.LearningStatus.ACCEPTED)
            """)
    boolean existsActiveDuplicate(@Param("learnerId") Long learnerId,
                                  @Param("teacherId") Long teacherId,
                                  @Param("skillId") Long skillId);

    long countByTeacherUsernameAndStatus(String teacherUsername, LearningStatus status);
}
