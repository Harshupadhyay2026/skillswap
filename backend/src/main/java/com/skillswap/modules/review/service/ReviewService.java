package com.skillswap.modules.review.service;

import com.skillswap.exception.BadRequestException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.learning.entity.LearningRequest;
import com.skillswap.modules.learning.entity.LearningStatus;
import com.skillswap.modules.learning.repository.LearningRequestRepository;
import com.skillswap.modules.review.dto.CreateReviewRequest;
import com.skillswap.modules.review.dto.PendingReviewItem;
import com.skillswap.modules.review.dto.ReviewResponse;
import com.skillswap.modules.review.entity.Review;
import com.skillswap.modules.review.repository.ReviewRepository;
import com.skillswap.modules.swap.entity.SkillSwapRequest;
import com.skillswap.modules.swap.entity.SwapStatus;
import com.skillswap.modules.swap.repository.SkillSwapRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reviews for completed interactions.
 *
 * A review can only exist for a swap or learning request that actually reached
 * COMPLETED, and only the two participants may leave one. That keeps ratings
 * meaningful — they cannot be manufactured.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final SkillSwapRepository swapRepository;
    private final LearningRequestRepository learningRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         SkillSwapRepository swapRepository,
                         LearningRequestRepository learningRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.swapRepository = swapRepository;
        this.learningRepository = learningRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse createReview(String username, CreateReviewRequest req) {
        boolean hasSwap = req.getSwapId() != null;
        boolean hasLearning = req.getLearningRequestId() != null;

        if (hasSwap == hasLearning) {
            throw new BadRequestException(
                    "Provide exactly one of swapId or learningRequestId");
        }

        User reviewer = getUser(username);
        Review review = new Review();
        review.setReviewer(reviewer);
        review.setRating(req.getRating());
        review.setComment(req.getComment() == null ? null : req.getComment().trim());

        if (hasSwap) {
            SkillSwapRequest swap = swapRepository.findById(req.getSwapId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Swap not found with id: " + req.getSwapId()));

            User counterpart = otherParticipant(reviewer, swap.getSender(), swap.getReceiver());
            if (swap.getStatus() != SwapStatus.COMPLETED) {
                throw new BadRequestException(
                        "You can only review a swap that has been completed");
            }
            if (reviewRepository.existsByReviewerIdAndSwapId(reviewer.getId(), swap.getId())) {
                throw new BadRequestException("You have already reviewed this swap");
            }

            review.setReviewee(counterpart);
            review.setSwap(swap);
        } else {
            LearningRequest learning = learningRepository.findById(req.getLearningRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Learning request not found with id: " + req.getLearningRequestId()));

            User counterpart = otherParticipant(reviewer,
                    learning.getLearner(), learning.getTeacher());

            if (learning.getStatus() != LearningStatus.COMPLETED) {
                throw new BadRequestException(
                        "You can only review a session that has been completed");
            }
            if (reviewRepository.existsByReviewerIdAndLearningRequestId(
                    reviewer.getId(), learning.getId())) {
                throw new BadRequestException("You have already reviewed this session");
            }

            review.setReviewee(counterpart);
            review.setLearningRequest(learning);
        }

        return ReviewResponse.fromEntity(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return reviewRepository.findTop20ByRevieweeIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReviewResponse::fromEntity)
                .toList();
    }

    /**
     * Completed interactions the caller has not yet reviewed, newest first.
     * Powers the "how was your session?" prompt.
     */
    @Transactional(readOnly = true)
    public List<PendingReviewItem> getPendingReviews(String username) {
        User me = getUser(username);

        List<PendingReviewItem> pending = new ArrayList<>();

        swapRepository.findAllUserSwaps(username).stream()
                .filter(s -> s.getStatus() == SwapStatus.COMPLETED)
                .filter(s -> !reviewRepository.existsByReviewerIdAndSwapId(me.getId(), s.getId()))
                .forEach(s -> {
                    boolean isSender = s.getSender().getId().equals(me.getId());
                    User counterpart = isSender ? s.getReceiver() : s.getSender();
                    PendingReviewItem item = new PendingReviewItem();
                    item.setType("SWAP");
                    item.setContextId(s.getId());
                    item.setCounterpartId(counterpart.getId());
                    item.setCounterpartName(counterpart.getFullName());
                    item.setSkillName(isSender
                            ? (s.getRequestedSkill() != null ? s.getRequestedSkill().getName() : null)
                            : (s.getOfferedSkill() != null ? s.getOfferedSkill().getName() : null));
                    item.setCompletedAt(s.getUpdatedAt());
                    pending.add(item);
                });

        learningRepository.findAllMine(username).stream()
                .filter(l -> l.getStatus() == LearningStatus.COMPLETED)
                .filter(l -> !reviewRepository.existsByReviewerIdAndLearningRequestId(
                        me.getId(), l.getId()))
                .forEach(l -> {
                    boolean isLearner = l.getLearner().getId().equals(me.getId());
                    User counterpart = isLearner ? l.getTeacher() : l.getLearner();
                    PendingReviewItem item = new PendingReviewItem();
                    item.setType("LEARNING");
                    item.setContextId(l.getId());
                    item.setCounterpartId(counterpart.getId());
                    item.setCounterpartName(counterpart.getFullName());
                    item.setSkillName(l.getSkill() != null ? l.getSkill().getName() : null);
                    item.setCompletedAt(l.getUpdatedAt());
                    pending.add(item);
                });

        pending.sort(Comparator.comparing(
                PendingReviewItem::getCompletedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return pending;
    }

    /** avg + count for one user; null average when there are no reviews. */
    @Transactional(readOnly = true)
    public RatingSummary getRatingSummary(Long userId) {
        Double avg = reviewRepository.averageRating(userId);
        long count = reviewRepository.countByRevieweeId(userId);
        return new RatingSummary(avg == null ? null : round1(avg), count);
    }

    /** Bulk stats for listing pages: userId -> summary. */
    @Transactional(readOnly = true)
    public Map<Long, RatingSummary> getRatingSummaries() {
        return reviewRepository.ratingStats().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new RatingSummary(round1((Double) row[1]), (Long) row[2])));
    }

    private User otherParticipant(User reviewer, User a, User b) {        if (a.getId().equals(reviewer.getId())) return b;
        if (b.getId().equals(reviewer.getId())) return a;
        throw new BadRequestException("You can only review someone you had a session with");
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private User getUser(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + usernameOrEmail));
    }

    /** Small value object so callers do not depend on the repository tuple. */
    public record RatingSummary(Double average, long count) {}
}
