package com.skillswap.modules.swap.service;

import com.skillswap.exception.BadRequestException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.entity.UserSkill;
import com.skillswap.modules.skill.repository.SkillRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.notification.entity.NotificationType;
import com.skillswap.modules.notification.service.NotificationService;
import com.skillswap.modules.swap.dto.CreateSwapRequest;
import com.skillswap.modules.swap.dto.SwapResponse;
import com.skillswap.modules.swap.entity.SkillSwapRequest;
import com.skillswap.modules.swap.entity.SwapStatus;
import com.skillswap.modules.swap.repository.SkillSwapRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SwapService {

    private final SkillSwapRepository swapRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final NotificationService notificationService;

    public SwapService(SkillSwapRepository swapRepository,
                       UserRepository userRepository,
                       SkillRepository skillRepository,
                       UserSkillRepository userSkillRepository,
                       NotificationService notificationService) {
        this.swapRepository = swapRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public SwapResponse proposeSwap(String senderUsername, CreateSwapRequest req) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        User receiver = userRepository.findById(req.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + req.getReceiverId()));

        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException("You cannot propose a swap with yourself");
        }

        Skill offeredSkill = skillRepository.findById(req.getOfferedSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Offered skill not found with id: " + req.getOfferedSkillId()));

        Skill requestedSkill = skillRepository.findById(req.getRequestedSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Requested skill not found with id: " + req.getRequestedSkillId()));

        // Core business rule: sender must offer at least one skill they teach.
        List<UserSkill> senderTeachSkills = userSkillRepository.findByUserIdAndSkillType(sender.getId(), "TEACH");
        if (senderTeachSkills.isEmpty()) {
            throw new BadRequestException("You must offer at least one skill you can teach before requesting a skill exchange");
        }

        // The sender must actually be able to teach what they are offering.
        userSkillRepository.findByUserIdAndSkillIdAndSkillType(
                        sender.getId(), offeredSkill.getId(), "TEACH")
                .orElseThrow(() -> new BadRequestException(
                        "You must add '" + offeredSkill.getName() + "' as a TEACH skill before offering it"));

        // ...and the receiver must be able to teach what is being requested.
        userSkillRepository.findByUserIdAndSkillIdAndSkillType(
                        receiver.getId(), requestedSkill.getId(), "TEACH")
                .orElseThrow(() -> new BadRequestException(
                        receiver.getFullName() + " does not teach '" + requestedSkill.getName() + "'"));

        if (swapRepository.existsActiveDuplicate(
                sender.getId(), receiver.getId(), offeredSkill.getId(), requestedSkill.getId())) {
            throw new BadRequestException("You already have an active swap proposal with "
                    + receiver.getFullName() + " for these skills");
        }

        SkillSwapRequest swap = new SkillSwapRequest();
        swap.setSender(sender);
        swap.setReceiver(receiver);
        swap.setOfferedSkill(offeredSkill);
        swap.setRequestedSkill(requestedSkill);
        swap.setMessage(req.getMessage());
        swap.setScheduledAt(req.getScheduledAt());
        swap.setStatus(SwapStatus.PENDING);

        SkillSwapRequest saved = swapRepository.save(swap);

        notificationService.notify(receiver, NotificationType.SWAP_PROPOSED,
                "New swap request",
                sender.getFullName() + " wants to swap " + offeredSkill.getName()
                        + " for " + requestedSkill.getName() + ".",
                "/requests");

        return SwapResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SwapResponse> getIncomingSwaps(String username) {
        return swapRepository.findByReceiverUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(SwapResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SwapResponse> getOutgoingSwaps(String username) {
        return swapRepository.findBySenderUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(SwapResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SwapResponse> getAllMySwaps(String username) {
        return swapRepository.findAllUserSwaps(username)
                .stream()
                .map(SwapResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Fetches one swap for the video-room page. Non-participants get a 404
     * (not a 403) so the existence of someone else's room is not revealed.
     */
    @Transactional(readOnly = true)
    public SwapResponse getSwap(String username, Long swapId) {
        SkillSwapRequest swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Swap request not found with id: " + swapId));

        boolean participant = swap.getSender().getUsername().equals(username)
                || swap.getReceiver().getUsername().equals(username);

        if (!participant) {
            throw new ResourceNotFoundException("Swap request not found with id: " + swapId);
        }

        return SwapResponse.fromEntity(swap);
    }

    @Transactional
    public SwapResponse acceptSwap(String username, Long swapId) {
        SkillSwapRequest swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("Swap request not found with id: " + swapId));

        if (!swap.getReceiver().getUsername().equals(username)) {
            throw new BadRequestException("Only the recipient of this swap request can accept it");
        }

        if (swap.getStatus() != SwapStatus.PENDING) {
            throw new BadRequestException("Cannot accept a swap that is currently " + swap.getStatus());
        }

        swap.setStatus(SwapStatus.ACCEPTED);
        // Generate secure Jitsi room meeting link
        String roomName = "skillswap-" + UUID.randomUUID().toString().substring(0, 10);
        swap.setMeetingLink("https://meet.jit.si/" + roomName);

        SkillSwapRequest saved = swapRepository.save(swap);

        notificationService.notify(swap.getSender(), NotificationType.SWAP_ACCEPTED,
                "Swap accepted",
                swap.getReceiver().getFullName() + " accepted your swap. Your meeting room is ready.",
                "/requests");

        return SwapResponse.fromEntity(saved);
    }

    @Transactional
    public SwapResponse declineSwap(String username, Long swapId) {
        SkillSwapRequest swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("Swap request not found with id: " + swapId));

        if (!swap.getReceiver().getUsername().equals(username)) {
            throw new BadRequestException("Only the recipient of this swap request can decline it");
        }

        if (swap.getStatus() != SwapStatus.PENDING) {
            throw new BadRequestException("Cannot decline a swap that is currently " + swap.getStatus());
        }

        swap.setStatus(SwapStatus.DECLINED);
        SkillSwapRequest saved = swapRepository.save(swap);

        notificationService.notify(swap.getSender(), NotificationType.SWAP_DECLINED,
                "Swap declined",
                swap.getReceiver().getFullName() + " declined your swap request.",
                "/requests");

        return SwapResponse.fromEntity(saved);
    }

    @Transactional
    public SwapResponse completeSwap(String username, Long swapId) {
        SkillSwapRequest swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("Swap request not found with id: " + swapId));

        boolean isParticipant = swap.getSender().getUsername().equals(username)
                || swap.getReceiver().getUsername().equals(username);

        if (!isParticipant) {
            throw new BadRequestException("Only participants of this swap can mark it as completed");
        }

        if (swap.getStatus() != SwapStatus.ACCEPTED) {
            throw new BadRequestException("Only accepted swaps can be marked as completed");
        }

        swap.setStatus(SwapStatus.COMPLETED);
        SkillSwapRequest saved = swapRepository.save(swap);

        User other = swap.getSender().getUsername().equals(username)
                ? swap.getReceiver() : swap.getSender();
        notificationService.notify(other, NotificationType.SWAP_COMPLETED,
                "Swap completed",
                "Your swap for " + swap.getRequestedSkill().getName()
                        + " is complete. Leave a review!",
                "/requests");

        return SwapResponse.fromEntity(saved);
    }

    @Transactional
    public SwapResponse cancelSwap(String username, Long swapId) {
        SkillSwapRequest swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("Swap request not found with id: " + swapId));

        if (!swap.getSender().getUsername().equals(username)) {
            throw new BadRequestException("Only the sender can cancel this swap request");
        }

        if (swap.getStatus() != SwapStatus.PENDING) {
            throw new BadRequestException("Cannot cancel a swap that is already " + swap.getStatus());
        }

        swap.setStatus(SwapStatus.CANCELLED);
        SkillSwapRequest saved = swapRepository.save(swap);

        notificationService.notify(swap.getReceiver(), NotificationType.SWAP_CANCELLED,
                "Swap cancelled",
                swap.getSender().getFullName() + " cancelled the swap request.",
                "/requests");

        return SwapResponse.fromEntity(saved);
    }
}
