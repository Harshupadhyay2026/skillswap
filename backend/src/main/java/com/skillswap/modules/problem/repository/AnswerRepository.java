package com.skillswap.modules.problem.repository;

import com.skillswap.modules.problem.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByProblemIdOrderByAcceptedDescCreatedAtAsc(Long problemId);

    List<Answer> findByProblemIdAndHiddenFalseOrderByAcceptedDescCreatedAtAsc(Long problemId);

    Optional<Answer> findByIdAndProblemId(Long id, Long problemId);

    Optional<Answer> findByProblemIdAndAcceptedTrue(Long problemId);

    Optional<Answer> findByProblemIdAndAcceptedTrueAndHiddenFalse(Long problemId);

    long countByAuthorId(Long authorId);

    long countByAuthorIdAndHiddenFalse(Long authorId);

    long countByAuthorIdAndAcceptedTrue(Long authorId);

    long countByAuthorIdAndAcceptedTrueAndHiddenFalse(Long authorId);

    long countByProblemId(Long problemId);

    long countByProblemIdAndHiddenFalse(Long problemId);

    /** One query for every answer count on a page of problems. */
    @Query("""
            SELECT a.problem.id, COUNT(a)
            FROM Answer a
            WHERE a.problem.id IN :problemIds
              AND a.hidden = false
            GROUP BY a.problem.id
            """)
    List<Object[]> countAnswersByProblemIds(@Param("problemIds") Collection<Long> problemIds);

    @Query("""
            SELECT a.author.id, COUNT(a)
            FROM Answer a
            WHERE a.author.id IN :authorIds
              AND a.hidden = false
            GROUP BY a.author.id
            """)
    List<Object[]> countAnswersByAuthorIds(@Param("authorIds") Collection<Long> authorIds);

    @Query("""
            SELECT a.author.id, COUNT(a)
            FROM Answer a
            WHERE a.author.id IN :authorIds
              AND a.accepted = true
              AND a.hidden = false
            GROUP BY a.author.id
            """)
    List<Object[]> countAcceptedByAuthorIds(@Param("authorIds") Collection<Long> authorIds);

    boolean existsByProblemIdAndAuthorId(Long problemId, Long authorId);
}
