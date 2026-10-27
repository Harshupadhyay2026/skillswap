package com.skillswap.modules.community.service;

import com.skillswap.modules.community.dto.TopContributorResponse;
import com.skillswap.modules.problem.repository.AnswerRepository;
import com.skillswap.modules.problem.repository.ProblemRepository;
import com.skillswap.modules.review.service.ReviewService;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Public contributor leaderboard.
 *
 * Ranking is by Q&A reputation (10 points per accepted answer). Counts are
 * fetched in bulk for the whole board so the page costs a constant number of
 * queries rather than one per contributor.
 */
@Service
public class CommunityService {

    private static final int MAX_LIMIT = 50;

    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final ProblemRepository problemRepository;
    private final ReviewService reviewService;

    public CommunityService(UserRepository userRepository,
                            AnswerRepository answerRepository,
                            ProblemRepository problemRepository,
                            ReviewService reviewService) {
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
        this.problemRepository = problemRepository;
        this.reviewService = reviewService;
    }

    @Transactional(readOnly = true)
    public List<TopContributorResponse> topContributors(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        // Over-fetch a little so the "has contributed" filter still fills the board.
        List<User> ranked = userRepository
                .findAllByOrderByHelpfulPointsDescIdAsc(PageRequest.of(0, safeLimit * 4));

        List<User> contributors = ranked.stream()
                .filter(u -> u.getHelpfulPoints() > 0)
                .limit(safeLimit)
                .toList();

        if (contributors.isEmpty()) {
            return List.of();
        }

        List<Long> ids = contributors.stream().map(User::getId).toList();

        Map<Long, Long> answers = toCountMap(answerRepository.countAnswersByAuthorIds(ids));
        Map<Long, Long> accepted = toCountMap(answerRepository.countAcceptedByAuthorIds(ids));
        Map<Long, Long> questions = toCountMap(problemRepository.countByAuthorIds(ids));
        Map<Long, ReviewService.RatingSummary> ratings = reviewService.getRatingSummaries();

        return java.util.stream.IntStream.range(0, contributors.size())
                .mapToObj(i -> {
                    User u = contributors.get(i);
                    ReviewService.RatingSummary rating = ratings.get(u.getId());

                    TopContributorResponse row = new TopContributorResponse();
                    row.setRank(i + 1);
                    row.setUserId(u.getId());
                    row.setFullName(u.getFullName());
                    row.setUsername(u.getUsername());
                    row.setAvatarUrl(u.getAvatarUrl());
                    row.setCollege(u.getCollege());
                    row.setBranch(u.getBranch());
                    row.setYearOfStudy(u.getYearOfStudy());
                    row.setHelpfulPoints(u.getHelpfulPoints());
                    row.setAnswersGiven(answers.getOrDefault(u.getId(), 0L));
                    row.setAcceptedAnswers(accepted.getOrDefault(u.getId(), 0L));
                    row.setQuestionsAsked(questions.getOrDefault(u.getId(), 0L));
                    row.setAvgRating(rating == null ? null : rating.average());
                    row.setReviewCount(rating == null ? 0L : rating.count());
                    return row;
                })
                .toList();
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]));
    }
}
