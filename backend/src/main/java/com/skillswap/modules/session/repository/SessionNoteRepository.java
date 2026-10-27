package com.skillswap.modules.session.repository;

import com.skillswap.modules.session.entity.SessionNote;
import com.skillswap.modules.session.entity.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionNoteRepository extends JpaRepository<SessionNote, Long> {

    List<SessionNote> findBySessionTypeAndSessionIdOrderByCreatedAtAsc(
            SessionType sessionType, Long sessionId);

    Optional<SessionNote> findByIdAndSessionTypeAndSessionId(
            Long id, SessionType sessionType, Long sessionId);

    @Query("SELECT COUNT(n) FROM SessionNote n WHERE n.author.username = :username")
    long countByAuthorUsername(@Param("username") String username);

    Optional<SessionNote> findTopByAuthorUsernameOrderByCreatedAtDesc(String username);
}
