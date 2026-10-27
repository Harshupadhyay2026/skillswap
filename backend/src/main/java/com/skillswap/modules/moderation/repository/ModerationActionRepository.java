package com.skillswap.modules.moderation.repository;

import com.skillswap.modules.moderation.entity.ModerationAction;
import com.skillswap.modules.moderation.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {

    List<ModerationAction> findByReportIdOrderByCreatedAtAsc(Long reportId);

    List<ModerationAction> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ReportTargetType targetType, Long targetId);
}
