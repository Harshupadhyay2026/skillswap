package com.skillswap.modules.problem.service;

import com.skillswap.common.PageResponse;
import com.skillswap.exception.BadRequestException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.problem.dto.AnswerResponse;
import com.skillswap.modules.problem.dto.CreateAnswerRequest;
import com.skillswap.modules.problem.dto.CreateProblemRequest;
import com.skillswap.modules.problem.dto.ProblemResponse;
import com.skillswap.modules.problem.dto.ProblemSummaryResponse;
import com.skillswap.modules.problem.entity.Answer;
import com.skillswap.modules.problem.entity.Problem;
import com.skillswap.modules.problem.entity.ProblemStatus;
import com.skillswap.modules.problem.repository.AnswerRepository;
import com.skillswap.modules.problem.repository.ProblemRepository;
import com.skillswap.modules.notification.entity.NotificationType;
import com.skillswap.modules.notification.service.NotificationService;
import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.repository.SkillRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Public Q&A — the free help path.
 *
     * Questions are answered by whoever wants to help. Accepting an answer marks the problem solved and
 * awards the answerer reputation points.
 */
@Service
public class ProblemService {

    /** Points awarded to the author of an accepted answer. */
    private static final int ACCEPTED_ANSWER_POINTS = 10;

    private final ProblemRepository problemRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final NotificationService notificationService;

    public ProblemService(ProblemRepository problemRepository,
                          AnswerRepository answerRepository,
                          UserRepository userRepository,
                          SkillRepository skillRepository,
                          NotificationService notificationService) {
        this.problemRepository = problemRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.notificationService = notificationService;
    }

    // ──────────────────────────────────────────
    // Problems
    // ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<ProblemSummaryResponse> listProblems(String query,
                                                             String category,
                                                             String status,
                                                             int page,
                                                             int size) {
        ProblemStatus parsed = null;
        if (StringUtils.hasText(status)) {
            try {
                parsed = ProblemStatus.valueOf(status.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unknown status: " + status);
            }
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<Problem> results = problemRepository.search(
                StringUtils.hasText(query) ? query.trim() : null,
                StringUtils.hasText(category) ? category.trim() : null,
                parsed,
                PageRequest.of(safePage, safeSize));

        // One grouped count for the whole page instead of one per problem.
        List<Long> problemIds = results.getContent().stream().map(Problem::getId).toList();
        Map<Long, Long> answerCounts = problemIds.isEmpty()
                ? Map.of()
                : answerRepository.countAnswersByProblemIds(problemIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Long) row[1]));

        return PageResponse.from(results, p -> ProblemSummaryResponse.fromEntity(
                p, answerCounts.getOrDefault(p.getId(), 0L)));
    }

    /** Detail view. Reading a visible question bumps the view counter. */
    @Transactional
    public ProblemResponse getProblem(Long id, String viewerUsername) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + id));

        if (problem.isHidden()) {
            if (!canViewHidden(problem, viewerUsername)) {
                throw new ResourceNotFoundException("Problem not found with id: " + id);
            }
        } else {
            problem.setViews(problem.getViews() + 1);
            problemRepository.save(problem);
        }

        List<AnswerResponse> answers = answerRepository
                .findByProblemIdAndHiddenFalseOrderByAcceptedDescCreatedAtAsc(id)
                .stream()
                .map(AnswerResponse::fromEntity)
                .toList();

        return ProblemResponse.fromEntity(problem, answers);
    }

    @Transactional
    public ProblemResponse createProblem(String username, CreateProblemRequest req) {
        User author = getUser(username);

        Skill skill = null;
        if (req.getSkillId() != null) {
            skill = skillRepository.findById(req.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Skill not found with id: " + req.getSkillId()));
        }

        Problem problem = new Problem();
        problem.setAuthor(author);
        problem.setTitle(req.getTitle().trim());
        problem.setDescription(req.getDescription().trim());
        problem.setCategory(req.getCategory().trim());
        problem.setLevel(StringUtils.hasText(req.getLevel())
                ? req.getLevel().trim().toUpperCase(Locale.ROOT) : null);
        problem.setSkill(skill);
        problem.setStatus(ProblemStatus.OPEN);

        return ProblemResponse.fromEntity(problemRepository.save(problem), List.of());
    }

    /** Author closes their own question without accepting an answer. */
    @Transactional
    public ProblemResponse closeProblem(String username, Long problemId) {
        Problem problem = getOwnedProblem(username, problemId);
        problem.setStatus(ProblemStatus.CLOSED);
        return ProblemResponse.fromEntity(problemRepository.save(problem), answerList(problemId));
    }

    /** Author reopens a closed question. */
    @Transactional
    public ProblemResponse reopenProblem(String username, Long problemId) {
        Problem problem = getOwnedProblem(username, problemId);
        if (problem.getStatus() == ProblemStatus.SOLVED) {
            throw new BadRequestException(
                    "This question has an accepted answer and cannot be reopened");
        }
        problem.setStatus(ProblemStatus.OPEN);
        return ProblemResponse.fromEntity(problemRepository.save(problem), answerList(problemId));
    }

    @Transactional(readOnly = true)
    public List<ProblemSummaryResponse> getMyProblems(String username) {
        User user = getUser(username);
        return problemRepository.findByAuthorIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(p -> ProblemSummaryResponse.fromEntity(
                        p, answerRepository.countByProblemIdAndHiddenFalse(p.getId())))
                .toList();
    }

    // ──────────────────────────────────────────
    // Answers
    // ──────────────────────────────────────────

    @Transactional
    public AnswerResponse addAnswer(String username, Long problemId, CreateAnswerRequest req) {
        User author = getUser(username);
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found with id: " + problemId));

        if (problem.isHidden()) {
            throw new ResourceNotFoundException("Problem not found with id: " + problemId);
        }

        if (problem.getStatus() == ProblemStatus.CLOSED) {
            throw new BadRequestException("This question is closed and no longer accepts answers");
        }
        if (problem.getAuthor().getId().equals(author.getId())) {
            throw new BadRequestException("You cannot answer your own question");
        }
        if (answerRepository.existsByProblemIdAndAuthorId(problemId, author.getId())) {
            throw new BadRequestException("You have already answered this question");
        }

        Answer answer = new Answer();
        answer.setProblem(problem);
        answer.setAuthor(author);
        answer.setBody(req.getBody().trim());

        return AnswerResponse.fromEntity(answerRepository.save(answer));
    }

    /**
     * Author accepts one answer. Marks the problem SOLVED and awards the
     * answerer reputation. Only one answer can be accepted per problem.
     */
    @Transactional
    public ProblemResponse acceptAnswer(String username, Long problemId, Long answerId) {
        Problem problem = getOwnedProblem(username, problemId);

        if (problem.getStatus() == ProblemStatus.SOLVED) {
            throw new BadRequestException("This question already has an accepted answer");
        }

        Answer answer = answerRepository.findByIdAndProblemId(answerId, problemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Answer not found with id: " + answerId + " on problem " + problemId));

        if (problem.isHidden() || answer.isHidden()) {
            throw new ResourceNotFoundException("Answer not found with id: " + answerId
                    + " on problem " + problemId);
        }

        answer.setAccepted(true);
        answerRepository.save(answer);

        // Award reputation to the answerer.
        User answerer = answer.getAuthor();
        answerer.setHelpfulPoints(answerer.getHelpfulPoints() + ACCEPTED_ANSWER_POINTS);
        userRepository.save(answerer);

        problem.setStatus(ProblemStatus.SOLVED);
        problemRepository.save(problem);

        notificationService.notify(answerer, NotificationType.ANSWER_ACCEPTED,
                "Your answer was accepted",
                problem.getAuthor().getFullName() + " accepted your answer to \""
                        + problem.getTitle() + "\". +10 reputation.",
                "/qa/" + problemId);

        return ProblemResponse.fromEntity(problem, answerList(problemId));
    }

    // ──────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────

    private List<AnswerResponse> answerList(Long problemId) {
        return answerRepository.findByProblemIdAndHiddenFalseOrderByAcceptedDescCreatedAtAsc(problemId)
                .stream()
                .map(AnswerResponse::fromEntity)
                .toList();
    }

    private boolean canViewHidden(Problem problem, String viewerUsername) {
        if (viewerUsername == null) return false;

        User viewer = userRepository.findByUsernameOrEmail(viewerUsername, viewerUsername)
                .orElse(null);
        if (viewer == null) return false;

        if (viewer.getId().equals(problem.getAuthor().getId())) return true;

        return viewer.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    private Problem getOwnedProblem(String username, Long problemId) {
        User user = getUser(username);
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found with id: " + problemId));

        if (!problem.getAuthor().getId().equals(user.getId())) {
            throw new BadRequestException("Only the person who asked can do that");
        }
        return problem;
    }

    private User getUser(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + usernameOrEmail));
    }
}
