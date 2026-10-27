package com.skillswap.modules.chat.repository;

import com.skillswap.modules.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUserOneIdAndUserTwoId(Long userOneId, Long userTwoId);

    /** Most recent activity first; a fresh thread (no messages yet) sorts by creation. */
    @Query("""
            SELECT c FROM Conversation c
            WHERE c.userOne.id = :userId OR c.userTwo.id = :userId
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);
}
