package com.skillswap.modules.notification.service;

import jakarta.mail.Address;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the best-effort email path without touching a real SMTP server.
 * The HTML/plain-text message is built by Spring Mail and captured before send.
 */
class EmailServiceTest {

    @Test
    @DisplayName("Disabled email logs but never touches the mail sender")
    void disabledEmailDoesNotSend() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = service(sender, false, "");

        service.sendNotification(
                "bob@example.com", "Bob", "Swap accepted", "Your room is ready.", "/requests");

        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("A notification becomes a multipart email with a working app link")
    void notificationBuildsHtmlAndPlainTextEmail() throws Exception {
        CapturingMailSender sender = new CapturingMailSender();
        EmailService service = service(sender, true, "");

        service.sendNotification(
                "bob@example.com",
                "Bob",
                "Swap accepted",
                "Alice accepted your swap.\nYour meeting room is ready.",
                "/requests?tab=incoming");

        MimeMessage message = sender.sent.get();
        assertThat(message).isNotNull();
        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo("[SkillSwap] Swap accepted");
        assertThat(message.getAllRecipients())
                .extracting(Address::toString)
                .containsExactly("bob@example.com");
        assertThat(message.getFrom()[0].toString()).contains("mailer@example.com");
        assertThat(message.getContent()).isInstanceOf(Multipart.class);
        assertThat(message.getContentType()).startsWith("multipart/");
    }

    @Test
    @DisplayName("The test endpoint reports missing SMTP config instead of throwing")
    void testEmailReportsMissingConfig() {
        CapturingMailSender sender = new CapturingMailSender();
        sender.setHost("");
        EmailService service = service(sender, true, null);

        EmailService.TestResult result = service.sendTest("bob@example.com", "Bob");

        assertThat(result.sent()).isFalse();
        assertThat(result.message()).contains("SMTP is not configured");
    }

    @Test
    @DisplayName("An empty MAIL_FROM falls back to the SMTP username")
    void emptyFromFallsBackToUsername() throws Exception {
        CapturingMailSender sender = new CapturingMailSender();
        EmailService service = service(sender, true, null);

        service.sendTest("bob@example.com", "Bob");

        assertThat(sender.sent.get().getFrom()[0].toString()).contains("mailer@example.com");
    }

    @Test
    @DisplayName("Email addresses are masked for logs and API responses")
    void masksRecipientAddresses() {
        assertThat(EmailService.maskEmail("alice@example.com")).isEqualTo("a***@example.com");
        assertThat(EmailService.maskEmail("not-an-email")).isEqualTo("***");
        assertThat(EmailService.maskEmail(null)).isEmpty();
    }

    private EmailService service(JavaMailSender sender, boolean enabled, String from) {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return new EmailService(provider, enabled, from, "http://localhost:5173");
    }

    private static class CapturingMailSender extends JavaMailSenderImpl {
        private final AtomicReference<MimeMessage> sent = new AtomicReference<>();

        CapturingMailSender() {
            setHost("smtp.example.com");
            setUsername("mailer@example.com");
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            sent.set(mimeMessage);
        }
    }
}
