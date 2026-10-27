package com.skillswap.modules.notification.service;

import com.skillswap.common.PageResponse;
import com.skillswap.exception.BadRequestException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.notification.dto.NotificationResponse;
import com.skillswap.modules.notification.entity.Notification;
import com.skillswap.modules.notification.entity.NotificationType;
import com.skillswap.modules.notification.repository.NotificationRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;

/**
 * In-app notifications.
 *
 * Every state change that matters to another person funnels through
 * {@link #notify}. Delivery is stored first (so it is never lost), then an
 * email is attempted asynchronously when SMTP is configured.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /** Records a notification and queues the email. Never notifies the actor. */
    @Transactional
    public void notify(User recipient,
                       NotificationType type,
                       String title,
                       String body,
                       String link) {
        if (recipient == null) return;

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLink(link);
        notificationRepository.save(notification);

        // Send only after the transaction commits: a rolled-back action must
        // not produce an email for a notification that never existed.
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchEmail(recipient, title, body, link);
                }
            });
        } else {
            dispatchEmail(recipient, title, body, link);
        }
    }

    private void dispatchEmail(User recipient, String title, String body, String link) {
        try {
            emailService.sendNotification(
                    recipient.getEmail(),
                    recipient.getFullName(),
                    title,
                    body,
                    link);
        } catch (RuntimeException e) {
            // Queue saturation or shutdown must not break the original action.
            log.warn("Could not queue notification email for {}: {}",
                    EmailService.maskEmail(recipient.getEmail()), e.getMessage());
        }
    }

    /** Non-sensitive SMTP readiness for the notifications page. */
    @Transactional(readOnly = true)
    public Map<String, Object> emailStatus(String username) {
        User me = getUser(username);
        return Map.of(
                "enabled", emailService.isEnabled(),
                "configured", emailService.isConfigured(),
                "recipient", EmailService.maskEmail(me.getEmail()));
    }

    /** Sends a synchronous test email so SMTP problems are visible immediately. */
    public Map<String, Object> sendTestEmail(String username) {
        User me = getUser(username);
        EmailService.TestResult result = emailService.sendTest(me.getEmail(), me.getFullName());
        if (!result.sent()) {
            throw new BadRequestException(result.message());
        }
        return Map.of(
                "sent", true,
                "recipient", EmailService.maskEmail(me.getEmail()),
                "message", result.message());
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(String username, int page, int size) {
        User me = getUser(username);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<Notification> result = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(me.getId(), PageRequest.of(safePage, safeSize));

        return PageResponse.from(result, NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return notificationRepository.countByUserIdAndReadAtIsNull(getUser(username).getId());
    }

    @Transactional
    public NotificationResponse markRead(String username, Long id) {
        User me = getUser(username);
        Notification notification = notificationRepository.findByIdAndUserId(id, me.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with id: " + id));

        if (!notification.isRead()) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public int markAllRead(String username) {
        return notificationRepository.markAllRead(getUser(username).getId(), Instant.now());
    }

    private User getUser(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + usernameOrEmail));
    }
}
