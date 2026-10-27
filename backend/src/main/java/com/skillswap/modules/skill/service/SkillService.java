package com.skillswap.modules.skill.service;

import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.skill.dto.AddUserSkillRequest;
import com.skillswap.modules.skill.dto.SkillResponse;
import com.skillswap.modules.skill.dto.UserSkillResponse;
import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.entity.UserSkill;
import com.skillswap.modules.skill.repository.SkillRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.problem.repository.AnswerRepository;
import com.skillswap.modules.problem.repository.ProblemRepository;
import com.skillswap.modules.review.service.ReviewService;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserRepository userRepository;
    private final ReviewService reviewService;
    private final ProblemRepository problemRepository;
    private final AnswerRepository answerRepository;

    public SkillService(SkillRepository skillRepository,
                        UserSkillRepository userSkillRepository,
                        UserRepository userRepository,
                        ReviewService reviewService,
                        ProblemRepository problemRepository,
                        AnswerRepository answerRepository) {
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.userRepository = userRepository;
        this.reviewService = reviewService;
        this.problemRepository = problemRepository;
        this.answerRepository = answerRepository;
    }

    // ──────────────────────────────────────────
    // Skill Catalog
    // ──────────────────────────────────────────

    public List<SkillResponse> searchSkills(String query, String category) {
        boolean hasQuery = StringUtils.hasText(query);
        boolean hasCategory = StringUtils.hasText(category);

        List<Skill> results;

        if (hasQuery && hasCategory) {
            results = skillRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCase(query, category);
        } else if (hasQuery) {
            results = skillRepository.findByNameContainingIgnoreCase(query);
        } else if (hasCategory) {
            results = skillRepository.findByCategoryIgnoreCase(category);
        } else {
            results = skillRepository.findAll();
        }

        return results.stream().map(this::toSkillResponse).toList();
    }

    public List<String> getCategories() {
        return skillRepository.findAll()
                .stream()
                .map(Skill::getCategory)
                .distinct()
                .sorted()
                .toList();
    }

    // ──────────────────────────────────────────
    // User Skills
    // ──────────────────────────────────────────

    public List<UserSkillResponse> getMySkills(String username) {
        User user = getUserByUsernameOrEmail(username);
        return userSkillRepository.findByUserId(user.getId())
                .stream()
                .map(this::toUserSkillResponse)
                .toList();
    }

    @Transactional
    public UserSkillResponse addSkillToUser(String username, AddUserSkillRequest req) {
        User user = getUserByUsernameOrEmail(username);
        Skill skill = skillRepository.findById(req.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + req.getSkillId()));

        userSkillRepository.findByUserIdAndSkillIdAndSkillType(
                        user.getId(), skill.getId(), req.getSkillType())
                .ifPresent(us -> {
                    throw new IllegalArgumentException(
                            "You already have this skill as " + req.getSkillType());
                });

        UserSkill us = new UserSkill();
        us.setUser(user);
        us.setSkill(skill);
        us.setSkillType(req.getSkillType().toUpperCase());
        us.setProficiency(req.getProficiency().toUpperCase());

        return toUserSkillResponse(userSkillRepository.save(us));
    }

    @Transactional
    public void removeSkillFromUser(String username, Long userSkillId) {
        User user = getUserByUsernameOrEmail(username);
        UserSkill us = userSkillRepository.findById(userSkillId)
                .orElseThrow(() -> new ResourceNotFoundException("UserSkill not found: " + userSkillId));
        if (!us.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your skill");
        }
        userSkillRepository.delete(us);
    }

    // ──────────────────────────────────────────
    // Marketplace – browse teachers
    // ──────────────────────────────────────────

    public List<UserSkillResponse> searchTeachers(Long skillId, String query) {
        List<UserSkill> teachers = (skillId != null)
                ? userSkillRepository.findTeachersBySkillId(skillId)
                : userSkillRepository.findAllTeachers();

        // In-memory filter by query on skill name or teacher name
        if (StringUtils.hasText(query)) {
            String q = query.toLowerCase(Locale.ROOT);
            teachers = teachers.stream()
                    .filter(us -> us.getSkill().getName().toLowerCase(Locale.ROOT).contains(q)
                            || us.getUser().getFullName().toLowerCase(Locale.ROOT).contains(q))
                    .toList();
        }

        // One aggregate query for all ratings instead of N per teacher.
        Map<Long, ReviewService.RatingSummary> ratings = reviewService.getRatingSummaries();

        return teachers.stream()
                .map(us -> toUserSkillResponse(us, ratings.get(us.getUser().getId())))
                .toList();
    }

    /**
     * Find students who teach {@code wanted} and also want to learn {@code offered}.
     * Returns one row per matching teacher/skill.
     */
    public List<UserSkillResponse> findMutualSwapMatches(String username, String offered, String wanted) {
        User current = getUserByUsernameOrEmail(username);
        return userSkillRepository.findMutualSwapMatches(offered, wanted, current.getId())
                .stream()
                .map(this::toUserSkillResponse)
                .toList();
    }

    /** Public profile payload: user details plus their teach/learn skills. */
    public com.skillswap.modules.user.dto.PublicProfileResponse getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        ReviewService.RatingSummary rating = reviewService.getRatingSummary(userId);
        List<UserSkillResponse> skills = userSkillRepository.findAllByUserId(userId)
                .stream()
                .map(us -> toUserSkillResponse(us, rating))
                .toList();

        com.skillswap.modules.user.dto.PublicProfileResponse profile =
                new com.skillswap.modules.user.dto.PublicProfileResponse();
        profile.setUser(com.skillswap.modules.user.dto.PublicUserResponse.fromEntity(user));
        profile.setSkills(skills);
        profile.setTeachingCount(skills.stream().filter(s -> "TEACH".equals(s.getSkillType())).count());
        profile.setLearningCount(skills.stream().filter(s -> "LEARN".equals(s.getSkillType())).count());

        profile.setAvgRating(rating.average());
        profile.setReviewCount(rating.count());

        // Q&A reputation
        profile.setHelpfulPoints(user.getHelpfulPoints());
        profile.setQuestionsAsked(problemRepository.countByAuthorIdAndHiddenFalse(userId));
        profile.setAnswersGiven(answerRepository.countByAuthorIdAndHiddenFalse(userId));
        profile.setAcceptedAnswers(answerRepository.countByAuthorIdAndAcceptedTrueAndHiddenFalse(userId));

        return profile;
    }

    // ──────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────

    private User getUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + usernameOrEmail));
    }

    private SkillResponse toSkillResponse(Skill s) {
        return new SkillResponse(s.getId(), s.getName(), s.getCategory(), s.getIcon());
    }

    private UserSkillResponse toUserSkillResponse(UserSkill us) {
        return toUserSkillResponse(us, null);
    }

    private UserSkillResponse toUserSkillResponse(UserSkill us, ReviewService.RatingSummary rating) {
        UserSkillResponse r = new UserSkillResponse();
        r.setId(us.getId());
        r.setSkillId(us.getSkill().getId());
        r.setSkillName(us.getSkill().getName());
        r.setSkillCategory(us.getSkill().getCategory());
        r.setSkillIcon(us.getSkill().getIcon());
        r.setSkillType(us.getSkillType());
        r.setProficiency(us.getProficiency());
        r.setTeacherId(us.getUser().getId());
        r.setTeacherName(us.getUser().getFullName());
        r.setTeacherCollege(us.getUser().getCollege());
        r.setTeacherBranch(us.getUser().getBranch());
        r.setTeacherYear(us.getUser().getYearOfStudy());
        r.setTeacherCity(us.getUser().getCity());
        r.setTeacherBio(us.getUser().getBio());
        r.setTeacherAvatarUrl(us.getUser().getAvatarUrl());
        r.setTeacherHelpfulPoints(us.getUser().getHelpfulPoints());
        r.setTeacherLearningMode(us.getUser().getLearningMode());
        r.setTeacherJoinedAt(us.getUser().getCreatedAt());
        if (rating != null) {
            r.setAvgRating(rating.average());
            r.setReviewCount(rating.count());
        } else {
            r.setReviewCount(0L);
        }
        return r;
    }
}
