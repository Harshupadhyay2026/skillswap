package com.skillswap.modules.progress.service;

import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.learning.entity.LearningRequest;
import com.skillswap.modules.learning.entity.LearningStatus;
import com.skillswap.modules.learning.repository.LearningRequestRepository;
import com.skillswap.modules.progress.dto.ProgressResponse;
import com.skillswap.modules.progress.dto.SkillProgressResponse;
import com.skillswap.modules.session.entity.SessionNote;
import com.skillswap.modules.session.entity.SessionType;
import com.skillswap.modules.session.repository.SessionNoteRepository;
import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.entity.UserSkill;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.swap.entity.SkillSwapRequest;
import com.skillswap.modules.swap.entity.SwapStatus;
import com.skillswap.modules.swap.repository.SkillSwapRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the dashboard's learning-progress section from completed sessions.
 *
 * Counting rules:
 *  - A learning session adds one learned session for the learner.
 *  - A completed swap adds one learned session for each participant for the
 *    skill they received, and one taught session for the skill they gave.
 *  - Skills the user marked as LEARN appear at 0 sessions so a goal is visible
 *    before the first session.
 */
@Service
public class ProgressService {

    private static final int TARGET_SESSIONS = 5;

    private final LearningRequestRepository learningRepository;
    private final SkillSwapRepository swapRepository;
    private final UserSkillRepository userSkillRepository;
    private final SessionNoteRepository noteRepository;
    private final UserRepository userRepository;

    public ProgressService(LearningRequestRepository learningRepository,
                           SkillSwapRepository swapRepository,
                           UserSkillRepository userSkillRepository,
                           SessionNoteRepository noteRepository,
                           UserRepository userRepository) {
        this.learningRepository = learningRepository;
        this.swapRepository = swapRepository;
        this.userSkillRepository = userSkillRepository;
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProgressResponse getMyProgress(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Map<Long, SkillAccumulator> skills = new LinkedHashMap<>();

        // Start with explicit learning goals so zero-session skills still show up.
        for (UserSkill goal : userSkillRepository.findByUserIdAndSkillType(user.getId(), "LEARN")) {
            skills.computeIfAbsent(goal.getSkill().getId(), id -> new SkillAccumulator(goal.getSkill()));
        }

        int learningSessions = 0;
        int teachingSessions = 0;
        Instant lastActivity = null;

        for (LearningRequest request : learningRepository.findAllMine(username)) {
            if (request.getStatus() != LearningStatus.COMPLETED) continue;

            if (request.getLearner().getId().equals(user.getId())) {
                learningSessions++;
                lastActivity = latest(lastActivity, request.getUpdatedAt());
                addLearned(skills, request.getSkill(), request.getTeacher().getFullName(),
                        SessionType.LEARNING, request.getId(), request.getUpdatedAt());
            } else if (request.getTeacher().getId().equals(user.getId())) {
                teachingSessions++;
                lastActivity = latest(lastActivity, request.getUpdatedAt());
            }
        }

        for (SkillSwapRequest swap : swapRepository.findAllUserSwaps(username)) {
            if (swap.getStatus() != SwapStatus.COMPLETED) continue;

            lastActivity = latest(lastActivity, swap.getUpdatedAt());

            if (swap.getSender().getId().equals(user.getId())) {
                learningSessions++;
                teachingSessions++;
                addLearned(skills, swap.getRequestedSkill(), swap.getReceiver().getFullName(),
                        SessionType.SWAP, swap.getId(), swap.getUpdatedAt());
            } else if (swap.getReceiver().getId().equals(user.getId())) {
                learningSessions++;
                teachingSessions++;
                addLearned(skills, swap.getOfferedSkill(), swap.getSender().getFullName(),
                        SessionType.SWAP, swap.getId(), swap.getUpdatedAt());
            }
        }

        long notesWritten = noteRepository.countByAuthorUsername(username);
        SessionNote latestNote = noteRepository.findTopByAuthorUsernameOrderByCreatedAtDesc(username)
                .orElse(null);
        if (latestNote != null) {
            lastActivity = latest(lastActivity, latestNote.getCreatedAt());
        }

        List<SkillProgressResponse> rows = skills.values().stream()
                .map(SkillAccumulator::toResponse)
                .sorted(progressOrder())
                .toList();

        ProgressResponse response = new ProgressResponse();
        response.setLearningSessions(learningSessions);
        response.setTeachingSessions(teachingSessions);
        response.setSkillsInProgress(rows.size());
        response.setNotesWritten(notesWritten);
        response.setLastActivityAt(lastActivity);
        response.setSkills(new ArrayList<>(rows));
        return response;
    }

    private void addLearned(Map<Long, SkillAccumulator> skills, Skill skill,
                            String teacherName, SessionType type, Long sessionId, Instant when) {
        if (skill == null) return;
        SkillAccumulator accumulator =
                skills.computeIfAbsent(skill.getId(), id -> new SkillAccumulator(skill));
        accumulator.addSession(teacherName, type, sessionId, when);
    }

    private Instant latest(Instant current, Instant candidate) {
        if (candidate == null) return current;
        if (current == null || candidate.isAfter(current)) return candidate;
        return current;
    }

    private Comparator<SkillProgressResponse> progressOrder() {
        return (a, b) -> {
            if (a.getLastSessionAt() == null && b.getLastSessionAt() == null) {
                return a.getSkillName().compareToIgnoreCase(b.getSkillName());
            }
            if (a.getLastSessionAt() == null) return 1;
            if (b.getLastSessionAt() == null) return -1;

            int byDate = b.getLastSessionAt().compareTo(a.getLastSessionAt());
            if (byDate != 0) return byDate;

            int bySessions = Integer.compare(b.getCompletedSessions(), a.getCompletedSessions());
            if (bySessions != 0) return bySessions;

            return a.getSkillName().compareToIgnoreCase(b.getSkillName());
        };
    }

    private static final class SkillAccumulator {
        private final Skill skill;
        private int completedSessions;
        private Instant lastSessionAt;
        private SessionType latestSessionType;
        private Long latestSessionId;
        private final Set<String> teachers = new LinkedHashSet<>();

        private SkillAccumulator(Skill skill) {
            this.skill = skill;
        }

        private void addSession(String teacherName, SessionType type, Long sessionId, Instant when) {
            completedSessions++;
            if (teacherName != null && !teacherName.isBlank()) {
                teachers.add(teacherName);
            }
            if (when != null && (lastSessionAt == null || when.isAfter(lastSessionAt))) {
                lastSessionAt = when;
                latestSessionType = type;
                latestSessionId = sessionId;
            }
        }

        private SkillProgressResponse toResponse() {
            SkillProgressResponse res = new SkillProgressResponse();
            res.setSkillId(skill.getId());
            res.setSkillName(skill.getName());
            res.setSkillIcon(skill.getIcon());
            res.setSkillCategory(skill.getCategory());
            res.setCompletedSessions(completedSessions);
            res.setTargetSessions(TARGET_SESSIONS);
            res.setProgressPercent(Math.min(100,
                    (int) Math.round(completedSessions * 100.0 / TARGET_SESSIONS)));
            res.setStage(stageFor(completedSessions));
            res.setLastSessionAt(lastSessionAt);
            res.setLatestSessionType(latestSessionType);
            res.setLatestSessionId(latestSessionId);
            res.setTeachers(new ArrayList<>(teachers));
            return res;
        }

        private String stageFor(int sessions) {
            if (sessions >= 10) return "Mentor-ready";
            if (sessions >= 5) return "Confident";
            if (sessions >= 3) return "Building momentum";
            if (sessions >= 1) return "Getting started";
            return "Ready to begin";
        }
    }
}
