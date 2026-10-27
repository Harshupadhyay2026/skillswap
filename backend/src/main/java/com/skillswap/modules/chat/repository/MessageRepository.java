package com.skillswap.modules.chat.repository;

import com.skillswap.modules.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    List<Message> findByConversationIdAndIdLessThanOrderByIdDesc(
            Long conversationId, Long beforeId, Pageable pageable);

    /** Latest message per conversation, one query for the whole inbox. */
    @Query("""
            SELECT m FROM Message m
            WHERE m.id IN (
                SELECT MAX(m2.id) FROM Message m2
                WHERE m2.conversation.id IN :conversationIds
                GROUP BY m2.conversation.id
            )
            """)
    List<Message> findLatestForConversations(@Param("conversationIds") Collection<Long> conversationIds);

    @Query("""
            SELECT m.conversation.id, COUNT(m)
            FROM Message m
            WHERE m.conversation.id IN :conversationIds
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            GROUP BY m.conversation.id
            """)
    List<Object[]> countUnreadByConversationIds(
            @Param("conversationIds") Collection<Long> conversationIds,
            @Param("userId") Long userId);

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(Long conversationId, Long senderId);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.readAt IS NULL
              AND m.sender.id <> :userId
              AND (m.conversation.userOne.id = :userId OR m.conversation.userTwo.id = :userId)
            """)
    long countUnreadForUser(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE Message m
            SET m.readAt = :readAt
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            """)
    int markConversationRead(@Param("conversationId") Long conversationId,
                             @Param("userId") Long userId,
                             @Param("readAt") Instant readAt);
}
