package com.skillswap.modules.notification.service;

import com.skillswap.modules.notification.entity.Notification;
import com.skillswap.modules.notification.entity.NotificationType;
import com.skillswap.modules.notification.repository.NotificationRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves that the in-app record is written first and the email is a
 * best-effort mirror: queue failures never break the original action.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @InjectMocks private NotificationService notificationService;

    @Test
    @DisplayName("A notification is stored and mirrored to the recipient's email")
    void notificationIsStoredAndEmailed() {
        User bob = user("Bob Notify", "bob@example.com");

        notificationService.notify(
                bob,
                NotificationType.SWAP_ACCEPTED,
                "Swap accepted",
                "Alice accepted your swap.",
                "/requests");

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(bob);
        assertThat(saved.getValue().getType()).isEqualTo(NotificationType.SWAP_ACCEPTED);
        assertThat(saved.getValue().getTitle()).isEqualTo("Swap accepted");
        assertThat(saved.getValue().getBody()).isEqualTo("Alice accepted your swap.");
        assertThat(saved.getValue().getLink()).isEqualTo("/requests");

        verify(emailService).sendNotification(
                "bob@example.com",
                "Bob Notify",
                "Swap accepted",
                "Alice accepted your swap.",
                "/requests");
    }

    @Test
    @DisplayName("A full mail queue never rolls back the in-app notification")
    void rejectedEmailQueueDoesNotBreakNotification() {
        User bob = user("Bob Notify", "bob@example.com");
        doThrow(new TaskRejectedException("queue full"))
                .when(emailService)
                .sendNotification(any(), any(), any(), any(), any());

        assertThatCode(() -> notificationService.notify(
                bob,
                NotificationType.SWAP_PROPOSED,
                "New swap request",
                "Alice wants to swap Python for React.",
                "/requests"))
                .doesNotThrowAnyException();

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Email status exposes only a masked recipient address")
    void emailStatusMasksRecipient() {
        when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(
                Optional.of(user("Bob Notify", "bob@example.com")));
        when(emailService.isEnabled()).thenReturn(true);
        when(emailService.isConfigured()).thenReturn(true);

        Map<String, Object> status = notificationService.emailStatus("bob");

        assertThat(status).containsEntry("enabled", true);
        assertThat(status).containsEntry("configured", true);
        assertThat(status).containsEntry("recipient", "b***@example.com");
    }

    @Test
    @DisplayName("A failed test email surfaces the SMTP problem as a 400")
    void failedTestEmailIsReported() {
        when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(
                Optional.of(user("Bob Notify", "bob@example.com")));
        when(emailService.sendTest("bob@example.com", "Bob Notify"))
                .thenReturn(new EmailService.TestResult(false, "SMTP is not configured."));

        assertThatThrownBy(() -> notificationService.sendTestEmail("bob"))
                .isInstanceOf(com.skillswap.exception.BadRequestException.class)
                .hasMessageContaining("SMTP is not configured");
    }

    private User user(String name, String email) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        return user;
    }
}
