package com.skillswap.modules.problem.repository;

import com.skillswap.modules.problem.entity.Problem;
import com.skillswap.modules.problem.entity.ProblemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    /**
     * Filtered list. Any argument may be null, which disables that filter.
     * Kept as one query so search/category/status compose cleanly.
     */
    @Query("""
            SELECT p FROM Problem p
            WHERE p.hidden = false
              AND (:status IS NULL OR p.status = :status)
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:query IS NULL
                   OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<Problem> search(@Param("query") String query,
                         @Param("category") String category,
                         @Param("status") ProblemStatus status,
                         Pageable pageable);

    List<Problem> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

        java.util.Optional<Problem> findByTitle(String title);

    long countByAuthorId(Long authorId);

    long countByAuthorIdAndHiddenFalse(Long authorId);

    long countByAuthorIdAndStatus(Long authorId, ProblemStatus status);

    List<Problem> findTop5ByOrderByCreatedAtDesc();

    @Query("""
            SELECT p.author.id, COUNT(p)
            FROM Problem p
            WHERE p.author.id IN :authorIds
              AND p.hidden = false
            GROUP BY p.author.id
            """)
    List<Object[]> countByAuthorIds(@Param("authorIds") java.util.Collection<Long> authorIds);
}
