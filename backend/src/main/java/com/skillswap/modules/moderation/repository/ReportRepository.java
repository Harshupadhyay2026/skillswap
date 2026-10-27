package com.skillswap.modules.moderation.repository;

import com.skillswap.modules.moderation.entity.Report;
import com.skillswap.modules.moderation.entity.ReportStatus;
import com.skillswap.modules.moderation.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
            SELECT r FROM Report r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:targetType IS NULL OR r.targetType = :targetType)
            ORDER BY CASE r.priority
                         WHEN com.skillswap.modules.moderation.entity.ReportPriority.HIGH THEN 0
                         WHEN com.skillswap.modules.moderation.entity.ReportPriority.NORMAL THEN 1
                         ELSE 2
                     END ASC,
                     r.createdAt ASC
            """)
    Page<Report> search(@Param("status") ReportStatus status,
                        @Param("targetType") ReportTargetType targetType,
                        Pageable pageable);

    List<Report> findByTargetTypeAndTargetIdAndStatusIn(
            ReportTargetType targetType,
            Long targetId,
            Collection<ReportStatus> statuses);

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId,
            Collection<ReportStatus> statuses);

    long countByReporterIdAndCreatedAtAfter(Long reporterId, Instant after);

    long countByTargetTypeAndTargetIdAndStatusIn(
            ReportTargetType targetType,
            Long targetId,
            Collection<ReportStatus> statuses);

    long countByStatus(ReportStatus status);

    long countByStatusAndReviewedAtAfter(ReportStatus status, Instant after);
}
