package com.skillswap.modules.swap.repository;

import com.skillswap.modules.swap.entity.SkillSwapRequest;
import com.skillswap.modules.swap.entity.SwapStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillSwapRepository extends JpaRepository<SkillSwapRequest, Long> {

    List<SkillSwapRequest> findByReceiverUsernameOrderByCreatedAtDesc(String receiverUsername);

    List<SkillSwapRequest> findBySenderUsernameOrderByCreatedAtDesc(String senderUsername);

    List<SkillSwapRequest> findByReceiverUsernameAndStatusOrderByCreatedAtDesc(String receiverUsername, SwapStatus status);

    List<SkillSwapRequest> findBySenderUsernameAndStatusOrderByCreatedAtDesc(String senderUsername, SwapStatus status);

    @Query("SELECT s FROM SkillSwapRequest s WHERE s.sender.username = :username OR s.receiver.username = :username ORDER BY s.createdAt DESC")
    List<SkillSwapRequest> findAllUserSwaps(@Param("username") String username);

    /**
     * Detects an existing proposal for the same pair + skills that has not been
     * declined/cancelled, so users cannot spam duplicates.
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM SkillSwapRequest s
            WHERE s.sender.id = :senderId
              AND s.receiver.id = :receiverId
              AND s.offeredSkill.id = :offeredSkillId
              AND s.requestedSkill.id = :requestedSkillId
              AND s.status IN (com.skillswap.modules.swap.entity.SwapStatus.PENDING,
                               com.skillswap.modules.swap.entity.SwapStatus.ACCEPTED)
            """)
    boolean existsActiveDuplicate(@Param("senderId") Long senderId,
                                  @Param("receiverId") Long receiverId,
                                  @Param("offeredSkillId") Long offeredSkillId,
                                  @Param("requestedSkillId") Long requestedSkillId);

    long countByReceiverUsernameAndStatus(String receiverUsername, SwapStatus status);
}
