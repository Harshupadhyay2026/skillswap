package com.skillswap.modules.moderation.service;

import com.skillswap.common.PageResponse;
import com.skillswap.exception.BadRequestException;
import com.skillswap.exception.ConflictException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.moderation.dto.CreateReportRequest;
import com.skillswap.modules.moderation.dto.ModerationActionResponse;
import com.skillswap.modules.moderation.dto.ModerationStatsResponse;
import com.skillswap.modules.moderation.dto.ReportCreatedResponse;
import com.skillswap.modules.moderation.dto.ReportResponse;
import com.skillswap.modules.moderation.dto.ResolveReportRequest;
import com.skillswap.modules.moderation.entity.ModerationAction;
import com.skillswap.modules.moderation.entity.ModerationActionType;
import com.skillswap.modules.moderation.entity.Report;
import com.skillswap.modules.moderation.entity.ReportReason;
import com.skillswap.modules.moderation.entity.ReportStatus;
import com.skillswap.modules.moderation.entity.ReportTargetType;
import com.skillswap.modules.moderation.repository.ModerationActionRepository;
import com.skillswap.modules.moderation.repository.ReportRepository;
import com.skillswap.modules.notification.entity.NotificationType;
import com.skillswap.modules.notification.service.NotificationService;
import com.skillswap.modules.problem.entity.Answer;
import com.skillswap.modules.problem.entity.Problem;
import com.skillswap.modules.problem.entity.ProblemStatus;
import com.skillswap.modules.problem.repository.AnswerRepository;
import com.skillswap.modules.problem.repository.ProblemRepository;
import com.skillswap.modules.user.entity.Role;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Reporting and moderation.
 *
 * v1 intentionally covers public Q&A content and user profiles. Private
 * messages and reviews are left for a later phase because reading them has
 * different privacy implications.
 */
@Service
public class ModerationService {

    private static final int MAX_REPORTS_PER_DAY = 10;
    private static final int ACCEPTED_ANSWER_POINTS = 10;
    private static final List<ReportStatus> ACTIVE_STATUSES =
            List.of(ReportStatus.OPEN, ReportStatus.IN_REVIEW);

    private final ReportRepository reportRepository;
    private final ModerationActionRepository actionRepository;
    private final ProblemRepository problemRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ModerationService(ReportRepository reportRepository,
                             ModerationActionRepository actionRepository,
                             ProblemRepository problemRepository,
                             AnswerRepository answerRepository,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.actionRepository = actionRepository;
        this.problemRepository = problemRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ──────────────────────────────────────────
    // Reporting
    // ──────────────────────────────────────────

    @Transactional
    public ReportCreatedResponse createReport(String username, CreateReportRequest req) {
        User reporter = getUser(username);
        TargetInfo target = resolveTarget(req.getTargetType(), req.getTargetId());

        if (!target.exists()) {
            throw new ResourceNotFoundException("The reported item no longer exists.");
        }
        if (target.ownerId() == null) {
            throw new BadRequestException("This item cannot be reported.");
        }
        if (target.ownerId().equals(reporter.getId())) {
            throw new BadRequestException("You cannot report your own content or profile.");
        }
        if (target.hidden()) {
            throw new BadRequestException("This item is already hidden from the community.");
        }

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                reporter.getId(), req.getTargetType(), req.getTargetId(), ACTIVE_STATUSES)) {
            throw new ConflictException("You already have an open report for this item.");
        }

        Instant dayAgo = Instant.now().minusSeconds(24 * 60 * 60);
        if (reportRepository.countByReporterIdAndCreatedAtAfter(reporter.getId(), dayAgo)
                >= MAX_REPORTS_PER_DAY) {
            throw new ConflictException(
                    "You have submitted too many reports in the last 24 hours. Try again later.");
        }

        Report report = new Report();
        report.setReporterId(reporter.getId());
        report.setTargetType(req.getTargetType());
        report.setTargetId(req.getTargetId());
        report.setTargetOwnerId(target.ownerId());
        report.setReason(req.getReason());
        report.setPriority(req.getReason().priority());
        report.setDetails(StringUtils.hasText(req.getDetails()) ? req.getDetails().trim() : null);
        report.setStatus(ReportStatus.OPEN);

        Report saved = reportRepository.save(report);
        notifyAdmins(saved, reporter, target);

        return ReportCreatedResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ReportCreatedResponse> myReports(String username) {
        User reporter = getUser(username);
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporter.getId())
                .stream()
                .map(ReportCreatedResponse::fromEntity)
                .toList();
    }

    // ──────────────────────────────────────────
    // Admin queue
    // ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> listReports(String status,
                                                    String targetType,
                                                    int page,
                                                    int size) {
        ReportStatus parsedStatus = parseStatus(status);
        ReportTargetType parsedType = parseTargetType(targetType);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<Report> results = reportRepository.search(
                parsedStatus, parsedType, PageRequest.of(safePage, safeSize));

        return PageResponse.from(results, r -> toResponse(r, false));
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long id) {
        return toResponse(getReportEntity(id), true);
    }

    @Transactional
    public ReportResponse claimReport(String username, Long id) {
        User admin = getUser(username);
        Report report = getReportEntity(id);

        if (report.getStatus() != ReportStatus.OPEN) {
            throw new ConflictException("Only open reports can be claimed.");
        }

        report.setStatus(ReportStatus.IN_REVIEW);
        report.setReviewedById(admin.getId());
        reportRepository.save(report);
        return toResponse(report, true);
    }

    @Transactional
    public ReportResponse resolveReport(String username, Long id, ResolveReportRequest req) {
        User admin = getUser(username);
        Report report = getReportEntity(id);

        if (!report.getStatus().isOpen()) {
            throw new ConflictException("This report is already closed.");
        }

        ModerationActionType action = req.getAction();
        if (action == ModerationActionType.RESTORE_CONTENT
                || action == ModerationActionType.REACTIVATE_USER) {
            throw new BadRequestException(
                    "Restore actions are available from the resolved report, not the open queue.");
        }

        String note = StringUtils.hasText(req.getNote()) ? req.getNote().trim() : null;
        if ((action == ModerationActionType.HIDE_CONTENT
                || action == ModerationActionType.SUSPEND_USER)
                && (note == null || note.length() < 3)) {
            throw new BadRequestException("Add a short note explaining this decision.");
        }

        User targetOwner = report.getTargetOwnerId() == null
                ? null
                : userRepository.findById(report.getTargetOwnerId()).orElse(null);

        switch (action) {
            case DISMISS -> {
                report.setStatus(ReportStatus.DISMISSED);
            }
            case HIDE_CONTENT -> {
                if (report.getTargetType() == ReportTargetType.USER) {
                    throw new BadRequestException(
                            "Choose Suspend user for a profile report, or dismiss it.");
                }
                hideReportedContent(report);
                report.setStatus(ReportStatus.RESOLVED);
                notifyOwner(targetOwner, NotificationType.CONTENT_HIDDEN,
                        "Your content was hidden",
                        "A moderator hid your " + targetLabel(report.getTargetType())
                                + " after reviewing a report. Reason: " + reasonLabel(report.getReason())
                                + ". You can contact the moderators if you think this was a mistake.",
                        contentLink(report));
            }
            case SUSPEND_USER -> {
                if (targetOwner == null) {
                    throw new ResourceNotFoundException("The reported account no longer exists.");
                }
                if (targetOwner.getId().equals(admin.getId())) {
                    throw new BadRequestException("You cannot suspend your own account.");
                }
                if (isAdmin(targetOwner)) {
                    throw new BadRequestException("Moderator accounts cannot be suspended.");
                }

                suspend(targetOwner, note);
                report.setStatus(ReportStatus.RESOLVED);
                notifyOwner(targetOwner, NotificationType.ACCOUNT_SUSPENDED,
                        "Your account was suspended",
                        "A moderator suspended your account. Reason: " + note
                                + ". Contact the moderators if you think this was a mistake.",
                        null);
            }
            default -> throw new BadRequestException("Unsupported moderation action.");
        }

        report.setReviewedById(admin.getId());
        report.setReviewedAt(Instant.now());
        report.setResolutionNote(note);
        reportRepository.save(report);

        recordAction(report.getId(), admin.getId(), action,
                report.getTargetType(), report.getTargetId(), note);

        // One decision should clear duplicates for the same target.
        if (action != ModerationActionType.DISMISS) {
            resolveSiblingReports(report, admin);
        }

        return toResponse(report, true);
    }

    @Transactional
    public void restoreProblem(String username, Long problemId, String note) {
        User admin = getUser(username);
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found with id: " + problemId));

        problem.setHidden(false);
        problem.setHiddenAt(null);
        problemRepository.save(problem);

        recordAction(null, admin.getId(), ModerationActionType.RESTORE_CONTENT,
                ReportTargetType.PROBLEM, problemId, cleanNote(note));

        notifyOwner(problem.getAuthor(), NotificationType.CONTENT_RESTORED,
                "Your question was restored",
                "A moderator restored \"" + problem.getTitle() + "\" to the community.",
                "/qa/" + problemId);
    }

    @Transactional
    public void restoreAnswer(String username, Long answerId, String note) {
        User admin = getUser(username);
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Answer not found with id: " + answerId));

        answer.setHidden(false);
        answer.setHiddenAt(null);
        answerRepository.save(answer);

        recordAction(null, admin.getId(), ModerationActionType.RESTORE_CONTENT,
                ReportTargetType.ANSWER, answerId, cleanNote(note));

        notifyOwner(answer.getAuthor(), NotificationType.CONTENT_RESTORED,
                "Your answer was restored",
                "A moderator restored your answer on \"" + answer.getProblem().getTitle() + "\".",
                "/qa/" + answer.getProblem().getId());
    }

    @Transactional
    public void reactivateUser(String username, Long userId, String note) {
        User admin = getUser(username);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        if (isAdmin(target) && !target.getId().equals(admin.getId())) {
            throw new BadRequestException("Moderator accounts cannot be reactivated from here.");
        }

        target.setActive(true);
        target.setSuspensionReason(null);
        target.setSuspendedAt(null);
        userRepository.save(target);

        recordAction(null, admin.getId(), ModerationActionType.REACTIVATE_USER,
                ReportTargetType.USER, userId, cleanNote(note));

        notifyOwner(target, NotificationType.ACCOUNT_REACTIVATED,
                "Your account was reactivated",
                "A moderator reactivated your account. Welcome back.",
                "/dashboard");
    }

    @Transactional(readOnly = true)
    public ModerationStatsResponse stats() {
        ModerationStatsResponse res = new ModerationStatsResponse();
        res.setOpenReports(reportRepository.countByStatus(ReportStatus.OPEN));
        res.setInReviewReports(reportRepository.countByStatus(ReportStatus.IN_REVIEW));

        Instant startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        res.setResolvedToday(reportRepository.countByStatusAndReviewedAtAfter(
                ReportStatus.RESOLVED, startOfDay));
        res.setSuspendedUsers(userRepository.countByIsActiveFalse());
        return res;
    }

    // ──────────────────────────────────────────
    // Target lookup / moderation mechanics
    // ──────────────────────────────────────────

    private record TargetInfo(boolean exists,
                              Long ownerId,
                              String preview,
                              String context,
                              boolean hidden) {
        static TargetInfo missing() {
            return new TargetInfo(false, null, "This item no longer exists.", "", true);
        }
    }

    private TargetInfo resolveTarget(ReportTargetType type, Long targetId) {
        if (type == null || targetId == null) {
            throw new BadRequestException("Choose what you are reporting.");
        }

        return switch (type) {
            case PROBLEM -> {
                Problem p = problemRepository.findById(targetId).orElse(null);
                if (p == null) yield TargetInfo.missing();
                yield new TargetInfo(
                        true,
                        p.getAuthor().getId(),
                        truncate(p.getTitle() + " — " + p.getDescription(), 600),
                        "Question • " + p.getStatus().name() + " • " + p.getCategory(),
                        p.isHidden());
            }
            case ANSWER -> {
                Answer a = answerRepository.findById(targetId).orElse(null);
                if (a == null) yield TargetInfo.missing();
                boolean hidden = a.isHidden() || a.getProblem().isHidden();
                yield new TargetInfo(
                        true,
                        a.getAuthor().getId(),
                        truncate(a.getBody(), 600),
                        "Answer on \"" + truncate(a.getProblem().getTitle(), 120) + "\"",
                        hidden);
            }
            case USER -> {
                User u = userRepository.findById(targetId).orElse(null);
                if (u == null) yield TargetInfo.missing();
                yield new TargetInfo(
                        true,
                        u.getId(),
                        u.getFullName() + " (@" + u.getUsername() + ")",
                        "Profile • " + (u.getCollege() == null ? "No college" : u.getCollege()),
                        !u.isActive());
            }
        };
    }

    private void hideReportedContent(Report report) {
        if (report.getTargetType() == ReportTargetType.PROBLEM) {
            Problem problem = problemRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Problem not found with id: " + report.getTargetId()));
            if (!problem.isHidden()) {
                answerRepository.findByProblemIdAndAcceptedTrueAndHiddenFalse(problem.getId())
                        .ifPresent(this::rollbackAcceptedAnswer);
                problem.setHidden(true);
                problem.setHiddenAt(Instant.now());
                problemRepository.save(problem);
            }
            return;
        }

        if (report.getTargetType() == ReportTargetType.ANSWER) {
            Answer answer = answerRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Answer not found with id: " + report.getTargetId()));
            if (!answer.isHidden()) {
                rollbackAcceptedAnswer(answer);
                answer.setHidden(true);
                answer.setHiddenAt(Instant.now());
                answerRepository.save(answer);
            }
        }
    }

    /** Hiding accepted content removes the reputation it earned. */
    private void rollbackAcceptedAnswer(Answer answer) {
        if (!answer.isAccepted()) return;

        answer.setAccepted(false);
        answerRepository.save(answer);

        User author = answer.getAuthor();
        author.setHelpfulPoints(Math.max(0, author.getHelpfulPoints() - ACCEPTED_ANSWER_POINTS));
        userRepository.save(author);

        Problem problem = answer.getProblem();
        if (problem != null && problem.getStatus() == ProblemStatus.SOLVED) {
            problem.setStatus(ProblemStatus.OPEN);
            problemRepository.save(problem);
        }
    }

    private void suspend(User user, String note) {
        user.setActive(false);
        user.setSuspensionReason(note);
        user.setSuspendedAt(Instant.now());
        userRepository.save(user);
    }

    private void resolveSiblingReports(Report current, User admin) {
        List<Report> siblings = reportRepository.findByTargetTypeAndTargetIdAndStatusIn(
                current.getTargetType(), current.getTargetId(), ACTIVE_STATUSES);

        for (Report sibling : siblings) {
            if (sibling.getId().equals(current.getId())) continue;
            sibling.setStatus(ReportStatus.RESOLVED);
            sibling.setReviewedById(admin.getId());
            sibling.setReviewedAt(Instant.now());
            sibling.setResolutionNote("Resolved with report #" + current.getId());
            reportRepository.save(sibling);
        }
    }

    private void recordAction(Long reportId,
                              Long adminId,
                              ModerationActionType action,
                              ReportTargetType targetType,
                              Long targetId,
                              String note) {
        ModerationAction entry = new ModerationAction();
        entry.setReportId(reportId);
        entry.setAdminId(adminId);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setNote(note);
        actionRepository.save(entry);
    }

    // ──────────────────────────────────────────
    // DTO mapping / helpers
    // ──────────────────────────────────────────

    private ReportResponse toResponse(Report report, boolean includeActions) {
        TargetInfo target = resolveTarget(report.getTargetType(), report.getTargetId());
        User reporter = userRepository.findById(report.getReporterId()).orElse(null);

        ReportResponse res = new ReportResponse();
        res.setId(report.getId());
        res.setTargetType(report.getTargetType().name());
        res.setTargetId(report.getTargetId());
        res.setTargetPreview(target.preview());
        res.setTargetContext(target.context());
        res.setTargetExists(target.exists());
        res.setTargetHidden(target.hidden());
        res.setReason(report.getReason().name());
        res.setPriority(report.getPriority().name());
        res.setDetails(report.getDetails());
        res.setStatus(report.getStatus().name());
        res.setCreatedAt(report.getCreatedAt());
        res.setReviewedAt(report.getReviewedAt());
        res.setResolutionNote(report.getResolutionNote());

        if (reporter != null) {
            res.setReporterId(reporter.getId());
            res.setReporterName(reporter.getFullName());
            res.setReporterUsername(reporter.getUsername());
        }

        Long ownerId = target.ownerId() != null ? target.ownerId() : report.getTargetOwnerId();
        if (ownerId != null) {
            User owner = userRepository.findById(ownerId).orElse(null);
            if (owner != null) {
                res.setTargetOwnerId(owner.getId());
                res.setTargetOwnerName(owner.getFullName());
                res.setTargetOwnerUsername(owner.getUsername());
                res.setTargetOwnerAvatarUrl(owner.getAvatarUrl());
                res.setTargetOwnerActive(owner.isActive());
                res.setTargetOwnerAdmin(isAdmin(owner));
                res.setTargetOwnerSuspensionReason(owner.getSuspensionReason());
            }
        }

        res.setDuplicateCount(reportRepository.countByTargetTypeAndTargetIdAndStatusIn(
                report.getTargetType(), report.getTargetId(), ACTIVE_STATUSES));

        if (report.getReviewedById() != null) {
            userRepository.findById(report.getReviewedById())
                    .ifPresent(reviewer -> res.setReviewedByName(reviewer.getFullName()));
        }

        if (includeActions) {
            res.setActions(actionRepository.findByReportIdOrderByCreatedAtAsc(report.getId())
                    .stream()
                    .map(a -> ModerationActionResponse.fromEntity(
                            a,
                            userRepository.findById(a.getAdminId())
                                    .map(User::getFullName)
                                    .orElse("Moderator")))
                    .toList());
        }

        return res;
    }

    private void notifyAdmins(Report report, User reporter, TargetInfo target) {
        List<User> admins = userRepository.findAllByRoleName("ROLE_ADMIN");
        if (admins.isEmpty()) return;

        String title = "New moderation report";
        String body = reporter.getFullName() + " reported a " + targetLabel(report.getTargetType())
                + " (" + reasonLabel(report.getReason()) + "): " + truncate(target.preview(), 140);

        for (User admin : admins) {
            if (admin.getId().equals(reporter.getId())) continue;
            notificationService.notify(admin, NotificationType.REPORT_RECEIVED, title, body,
                    "/admin/reports?report=" + report.getId());
        }
    }

    private void notifyOwner(User owner,
                             NotificationType type,
                             String title,
                             String body,
                             String link) {
        if (owner != null) {
            notificationService.notify(owner, type, title, body, link);
        }
    }

    private String contentLink(Report report) {
        return switch (report.getTargetType()) {
            case PROBLEM -> "/qa/" + report.getTargetId();
            case ANSWER -> answerRepository.findById(report.getTargetId())
                    .map(a -> "/qa/" + a.getProblem().getId())
                    .orElse(null);
            case USER -> null;
        };
    }

    private String targetLabel(ReportTargetType type) {
        return switch (type) {
            case PROBLEM -> "question";
            case ANSWER -> "answer";
            case USER -> "profile";
        };
    }

    private String reasonLabel(ReportReason reason) {
        return switch (reason) {
            case SPAM -> "spam";
            case HARASSMENT -> "harassment";
            case HATE -> "hate speech";
            case MISINFORMATION -> "misinformation";
            case IMPERSONATION -> "impersonation";
            case OTHER -> "another reason";
        };
    }

    private ReportStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        try {
            return ReportStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown report status: " + status);
        }
    }

    private ReportTargetType parseTargetType(String targetType) {
        if (!StringUtils.hasText(targetType)) return null;
        try {
            return ReportTargetType.valueOf(targetType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown target type: " + targetType);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        String clean = value.trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 1) + "…";
    }

    private static String cleanNote(String note) {
        return StringUtils.hasText(note) ? note.trim() : null;
    }

    private static boolean isAdmin(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private Report getReportEntity(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Report not found with id: " + id));
    }

    private User getUser(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + usernameOrEmail));
    }
}
