package com.skillswap.modules.session.service;

import com.skillswap.exception.BadRequestException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.learning.entity.LearningRequest;
import com.skillswap.modules.learning.entity.LearningStatus;
import com.skillswap.modules.learning.repository.LearningRequestRepository;
import com.skillswap.modules.session.dto.SessionNoteRequest;
import com.skillswap.modules.session.dto.SessionNoteResponse;
import com.skillswap.modules.session.entity.SessionNote;
import com.skillswap.modules.session.entity.SessionType;
import com.skillswap.modules.session.repository.SessionNoteRepository;
import com.skillswap.modules.swap.entity.SkillSwapRequest;
import com.skillswap.modules.swap.entity.SwapStatus;
import com.skillswap.modules.swap.repository.SkillSwapRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Shared notes for a swap or learning session.
 *
 * Notes are one of the few pieces of session data both participants can edit,
 * so access is checked on every read and write: you must be a participant and
 * the session must have reached ACCEPTED (notes stay editable after COMPLETED).
 */
@Service
public class SessionNoteService {

    private final SessionNoteRepository noteRepository;
    private final SkillSwapRepository swapRepository;
    private final LearningRequestRepository learningRepository;
    private final UserRepository userRepository;

    public SessionNoteService(SessionNoteRepository noteRepository,
                              SkillSwapRepository swapRepository,
                              LearningRequestRepository learningRepository,
                              UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.swapRepository = swapRepository;
        this.learningRepository = learningRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SessionNoteResponse> list(String username, String sessionType, Long sessionId) {
        SessionType type = parseType(sessionType);
        requireParticipantAndUnlocked(username, type, sessionId);

        return noteRepository.findBySessionTypeAndSessionIdOrderByCreatedAtAsc(type, sessionId)
                .stream()
                .map(SessionNoteResponse::fromEntity)
                .toList();
    }

    @Transactional
    public SessionNoteResponse create(String username, String sessionType,
                                      Long sessionId, SessionNoteRequest req) {
        SessionType type = parseType(sessionType);
        requireParticipantAndUnlocked(username, type, sessionId);

        User author = getUser(username);

        SessionNote note = new SessionNote();
        note.setSessionType(type);
        note.setSessionId(sessionId);
        note.setAuthor(author);
        note.setTitle(normalizeTitle(req.getTitle()));
        note.setContent(req.getContent().trim());
        note.setNextStep(normalizeOptional(req.getNextStep()));

        return SessionNoteResponse.fromEntity(noteRepository.save(note));
    }

    @Transactional
    public SessionNoteResponse update(String username, String sessionType, Long sessionId,
                                      Long noteId, SessionNoteRequest req) {
        SessionType type = parseType(sessionType);
        requireParticipantAndUnlocked(username, type, sessionId);

        SessionNote note = getNote(type, sessionId, noteId);
        requireAuthor(note, username);

        note.setTitle(normalizeTitle(req.getTitle()));
        note.setContent(req.getContent().trim());
        note.setNextStep(normalizeOptional(req.getNextStep()));

        return SessionNoteResponse.fromEntity(noteRepository.save(note));
    }

    @Transactional
    public void delete(String username, String sessionType, Long sessionId, Long noteId) {
        SessionType type = parseType(sessionType);
        requireParticipantAndUnlocked(username, type, sessionId);

        SessionNote note = getNote(type, sessionId, noteId);
        requireAuthor(note, username);

        noteRepository.delete(note);
    }

    private SessionNote getNote(SessionType type, Long sessionId, Long noteId) {
        return noteRepository.findByIdAndSessionTypeAndSessionId(noteId, type, sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Session note not found with id: " + noteId));
    }

    private void requireAuthor(SessionNote note, String username) {
        if (!note.getAuthor().getUsername().equals(username)) {
            throw new AccessDeniedException("Only the person who wrote this note can edit or delete it");
        }
    }

    private void requireParticipantAndUnlocked(String username, SessionType type, Long sessionId) {
        if (type == SessionType.SWAP) {
            SkillSwapRequest swap = swapRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Swap session not found with id: " + sessionId));

            boolean participant = swap.getSender().getUsername().equals(username)
                    || swap.getReceiver().getUsername().equals(username);
            if (!participant) {
                throw new ResourceNotFoundException("Swap session not found with id: " + sessionId);
            }
            if (swap.getStatus() != SwapStatus.ACCEPTED && swap.getStatus() != SwapStatus.COMPLETED) {
                throw new BadRequestException("Session notes unlock once the swap is accepted.");
            }
            return;
        }

        LearningRequest request = learningRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Learning session not found with id: " + sessionId));

        boolean participant = request.getLearner().getUsername().equals(username)
                || request.getTeacher().getUsername().equals(username);
        if (!participant) {
            throw new ResourceNotFoundException("Learning session not found with id: " + sessionId);
        }
        if (request.getStatus() != LearningStatus.ACCEPTED
                && request.getStatus() != LearningStatus.COMPLETED) {
            throw new BadRequestException("Session notes unlock once the learning request is accepted.");
        }
    }

    private SessionType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Session type is required");
        }
        try {
            return SessionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported session type: " + raw);
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String normalizeTitle(String title) {
        String value = title == null ? "" : title.trim();
        return value.isEmpty() ? "Session note" : value;
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
