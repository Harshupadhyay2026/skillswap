package com.skillswap.modules.learning.service;

import com.skillswap.exception.BadRequestException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.learning.dto.CreateLearningRequest;
import com.skillswap.modules.learning.dto.LearningRequestResponse;
import com.skillswap.modules.learning.entity.LearningRequest;
import com.skillswap.modules.learning.entity.LearningStatus;
import com.skillswap.modules.learning.repository.LearningRequestRepository;
import com.skillswap.modules.notification.entity.NotificationType;
import com.skillswap.modules.notification.service.NotificationService;
import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.entity.UserSkill;
import com.skillswap.modules.skill.repository.SkillRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 100% free learning requests.
 *
 * Any authenticated user can request a free shared session for a skill.
 */
@Service
public class LearningService {

    private final LearningRequestRepository learningRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final NotificationService notificationService;

    public LearningService(LearningRequestRepository learningRepository,
                           UserRepository userRepository,
                           SkillRepository skillRepository,
                           UserSkillRepository userSkillRepository,
                           NotificationService notificationService) {
        this.learningRepository = learningRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public LearningRequestResponse createRequest(String learnerUsername, CreateLearningRequest req) {
        User learner = getUser(learnerUsername);

        User teacher = userRepository.findById(req.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: " + req.getTeacherId()));

        if (learner.getId().equals(teacher.getId())) {
            throw new BadRequestException("You cannot request to learn from yourself");
        }

        Skill skill = skillRepository.findById(req.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Skill not found with id: " + req.getSkillId()));

        // The teacher must actually offer this skill.
        UserSkill teaching = userSkillRepository
                .findByUserIdAndSkillIdAndSkillType(teacher.getId(), skill.getId(), "TEACH")
                .orElseThrow(() -> new BadRequestException(
                        teacher.getFullName() + " does not teach '" + skill.getName() + "'"));

        if (learningRepository.existsActiveDuplicate(
                learner.getId(), teacher.getId(), skill.getId())) {
            throw new BadRequestException("You already have an active learning request with "
                    + teacher.getFullName() + " for '" + skill.getName() + "'");
        }

        LearningRequest request = new LearningRequest();
        request.setLearner(learner);
        request.setTeacher(teacher);
        request.setSkill(skill);
        request.setMessage(req.getMessage());
        request.setScheduledAt(req.getScheduledAt());
        request.setStatus(LearningStatus.PENDING);

        LearningRequest saved = learningRepository.save(request);

        notificationService.notify(teacher, NotificationType.LEARNING_REQUESTED,
                "New learning request",
                learner.getFullName() + " wants to learn " + skill.getName() + " from you.",
                "/requests");

        return LearningRequestResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<LearningRequestResponse> getIncoming(String username) {
        return learningRepository.findIncoming(username).stream()
                .map(LearningRequestResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LearningRequestResponse> getOutgoing(String username) {
        return learningRepository.findOutgoing(username).stream()
                .map(LearningRequestResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LearningRequestResponse> getAllMine(String username) {
        return learningRepository.findAllMine(username).stream()
                .map(LearningRequestResponse::fromEntity)
                .toList();
    }

    /**
     * Fetches one learning session for the video-room page. Non-participants
     * get a 404 so another user's room is not exposed.
     */
    @Transactional(readOnly = true)
    public LearningRequestResponse getMine(String username, Long id) {
        LearningRequest request = getRequest(id);

        boolean participant = request.getLearner().getUsername().equals(username)
                || request.getTeacher().getUsername().equals(username);

        if (!participant) {
            throw new ResourceNotFoundException("Learning request not found with id: " + id);
        }

        return LearningRequestResponse.fromEntity(request);
    }

    @Transactional
    public LearningRequestResponse accept(String username, Long id) {
        LearningRequest request = getRequest(id);

        if (!request.getTeacher().getUsername().equals(username)) {
            throw new BadRequestException("Only the teacher can accept this learning request");
        }
        if (request.getStatus() != LearningStatus.PENDING) {
            throw new BadRequestException("Cannot accept a request that is " + request.getStatus());
        }

        request.setStatus(LearningStatus.ACCEPTED);
        String room = "skillswap-learn-" + UUID.randomUUID().toString().substring(0, 10);
        request.setMeetingLink("https://meet.jit.si/" + room);

        LearningRequest saved = learningRepository.save(request);

        notificationService.notify(request.getLearner(), NotificationType.LEARNING_ACCEPTED,
                "Learning request accepted",
                request.getTeacher().getFullName() + " accepted your request to learn "
                        + request.getSkill().getName() + ".",
                "/requests");

        return LearningRequestResponse.fromEntity(saved);
    }

    @Transactional
    public LearningRequestResponse decline(String username, Long id) {
        LearningRequest request = getRequest(id);

        if (!request.getTeacher().getUsername().equals(username)) {
            throw new BadRequestException("Only the teacher can decline this learning request");
        }
        if (request.getStatus() != LearningStatus.PENDING) {
            throw new BadRequestException("Cannot decline a request that is " + request.getStatus());
        }

        request.setStatus(LearningStatus.DECLINED);
        LearningRequest saved = learningRepository.save(request);

        notificationService.notify(request.getLearner(), NotificationType.LEARNING_DECLINED,
                "Learning request declined",
                request.getTeacher().getFullName() + " declined your request to learn "
                        + request.getSkill().getName() + ".",
                "/requests");

        return LearningRequestResponse.fromEntity(saved);
    }

    @Transactional
    public LearningRequestResponse complete(String username, Long id) {
        LearningRequest request = getRequest(id);

        boolean participant = request.getLearner().getUsername().equals(username)
                || request.getTeacher().getUsername().equals(username);
        if (!participant) {
            throw new BadRequestException("Only participants can complete this session");
        }
        if (request.getStatus() != LearningStatus.ACCEPTED) {
            throw new BadRequestException("Only accepted sessions can be completed");
        }

        request.setStatus(LearningStatus.COMPLETED);
        LearningRequest saved = learningRepository.save(request);

        User other = request.getLearner().getUsername().equals(username)
                ? request.getTeacher() : request.getLearner();
        notificationService.notify(other, NotificationType.LEARNING_COMPLETED,
                "Session completed",
                "Your " + request.getSkill().getName()
                        + " session is complete. Leave a review!",
                "/requests");

        return LearningRequestResponse.fromEntity(saved);
    }

    @Transactional
    public LearningRequestResponse cancel(String username, Long id) {
        LearningRequest request = getRequest(id);

        if (!request.getLearner().getUsername().equals(username)) {
            throw new BadRequestException("Only the learner can cancel this request");
        }
        if (request.getStatus() != LearningStatus.PENDING) {
            throw new BadRequestException("Cannot cancel a request that is " + request.getStatus());
        }

        request.setStatus(LearningStatus.CANCELLED);
        LearningRequest saved = learningRepository.save(request);

        notificationService.notify(request.getTeacher(), NotificationType.LEARNING_CANCELLED,
                "Learning request cancelled",
                request.getLearner().getFullName() + " cancelled their request to learn "
                        + request.getSkill().getName() + ".",
                "/requests");

        return LearningRequestResponse.fromEntity(saved);
    }

    private LearningRequest getRequest(Long id) {
        return learningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Learning request not found with id: " + id));
    }

    private User getUser(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + usernameOrEmail));
    }
}
