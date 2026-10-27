package com.skillswap.modules.review.repository;

import com.skillswap.modules.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

    boolean existsByReviewerIdAndSwapId(Long reviewerId, Long swapId);

    boolean existsByReviewerIdAndLearningRequestId(Long reviewerId, Long learningRequestId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.id = :userId")
    Double averageRating(@Param("userId") Long userId);

    long countByRevieweeId(Long userId);

    /**
     * One query for the whole marketplace: returns (userId, avgRating, count)
     * rows so listing pages do not fire a query per teacher.
     */
    @Query("""
            SELECT r.reviewee.id, AVG(r.rating), COUNT(r)
            FROM Review r
            GROUP BY r.reviewee.id
            """)
    List<Object[]> ratingStats();

    /** Latest reviews written about a user, capped for profile display. */
    List<Review> findTop20ByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);
}
