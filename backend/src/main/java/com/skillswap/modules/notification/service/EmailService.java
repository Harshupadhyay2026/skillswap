package com.skillswap.modules.notification.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Best-effort notification email.
 *
 * In-app notifications are the source of truth; this service mirrors them to
 * the recipient's registered email address when SMTP is configured. Sending
 * runs on a small async pool and every failure is swallowed so a mail outage
 * can never roll back the swap, session, answer, or moderation action that
 * triggered the notification.
 *
 * A synchronous {@link #sendTest} path exists so the UI can verify SMTP
 * settings and show a useful error immediately.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;
    private final String frontendUrl;
    private final AtomicBoolean missingConfigLogged = new AtomicBoolean(false);

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${app.mail.enabled:true}") boolean enabled,
                        @Value("${app.mail.from:}") String from,
                        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    /** Mirrors one in-app notification to email. Never throws to the caller. */
    @Async("notificationEmailExecutor")
    public void sendNotification(String to,
                                 String recipientName,
                                 String title,
                                 String body,
                                 String link) {
        send(to, recipientName, title, body, link);
    }

    /**
     * Synchronous test email used by the "Send test email" action. Unlike
     * notifications, the result is returned so SMTP problems are visible.
     */
    public TestResult sendTest(String to, String recipientName) {
        return send(to,
                recipientName,
                "Email notifications are working",
                "This is a test email from SkillSwap. Future swap, session and "
                        + "community notifications will also arrive here.",
                "/notifications");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** True when email is enabled and an SMTP sender with a host is available. */
    public boolean isConfigured() {
        if (!enabled) return false;
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) return false;
        if (sender instanceof JavaMailSenderImpl impl) {
            return StringUtils.hasText(impl.getHost());
        }
        return true;
    }

    /** Masks an address for logs and API responses: a***@example.com. */
    public static String maskEmail(String email) {
        if (!StringUtils.hasText(email)) return "";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    private TestResult send(String to,
                            String recipientName,
                            String title,
                            String body,
                            String link) {
        if (!StringUtils.hasText(to)) {
            log.debug("Skipping notification email '{}': recipient has no address", title);
            return new TestResult(false, "Your account does not have an email address.");
        }

        if (!enabled) {
            log.debug("[mail disabled] to={} subject={}", maskEmail(to), title);
            return new TestResult(false, "Email delivery is disabled (MAIL_ENABLED=false).");
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || (sender instanceof JavaMailSenderImpl impl
                && !StringUtils.hasText(impl.getHost()))) {
            if (missingConfigLogged.compareAndSet(false, true)) {
                log.warn("Email notifications are enabled but MAIL_HOST is not configured — "
                        + "in-app notifications will continue without email. "
                        + "Set MAIL_HOST, MAIL_USERNAME and MAIL_PASSWORD in backend/.env.");
            }
            return new TestResult(false,
                    "SMTP is not configured. Set MAIL_HOST, MAIL_USERNAME and MAIL_PASSWORD.");
        }

        try {
            sender.send(buildMessage(sender, to, recipientName, title, body, link));
            log.info("Notification email sent to {}", maskEmail(to));
            return new TestResult(true, "Test email sent to " + maskEmail(to) + ".");
        } catch (Exception e) {
            // Deliberately non-fatal — the notification is already stored in-app.
            log.warn("Could not send notification email to {}: {}", maskEmail(to), e.getMessage());
            return new TestResult(false,
                    "SMTP delivery failed. Check the backend log and your SMTP settings.");
        }
    }

    private MimeMessage buildMessage(JavaMailSender sender,
                                     String to,
                                     String recipientName,
                                     String title,
                                     String body,
                                     String link) throws Exception {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String senderAddress = resolveFrom(sender);
        helper.setFrom(senderAddress, "SkillSwap");
        helper.setTo(to);
        helper.setSubject("[SkillSwap] " + title);

        String name = StringUtils.hasText(recipientName) ? recipientName : "there";
        String url = absoluteLink(link);
        helper.setText(plainText(name, title, body, url), htmlEmail(name, title, body, url));
        return message;
    }

    private String resolveFrom(JavaMailSender sender) {
        if (StringUtils.hasText(from)) return from;
        if (sender instanceof JavaMailSenderImpl impl && StringUtils.hasText(impl.getUsername())) {
            return impl.getUsername();
        }
        return "no-reply@skillswap.local";
    }

    private String absoluteLink(String link) {
        String base = frontendUrl == null ? "" : frontendUrl.replaceAll("/+$", "");
        if (!StringUtils.hasText(link)) return base;
        if (link.startsWith("http://") || link.startsWith("https://")) return link;
        return base + (link.startsWith("/") ? "" : "/") + link;
    }

    private String plainText(String name, String title, String body, String url) {
        return "Hi " + name + ",\n\n"
                + title + "\n\n"
                + body + "\n\n"
                + "Open SkillSwap: " + url + "\n\n"
                + "— SkillSwap\n"
                + "You are receiving this because you have a SkillSwap account.";
    }

    private String htmlEmail(String name, String title, String body, String url) {
        String safeName = HtmlUtils.htmlEscape(name);
        String safeTitle = HtmlUtils.htmlEscape(title);
        String safeBody = HtmlUtils.htmlEscape(body).replace("\n", "<br>");
        String safeUrl = HtmlUtils.htmlEscape(url);

        return """
                <!doctype html>
                <html lang="en">
                  <body style="margin:0;padding:0;background:#f5f3ee;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1f2937;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f5f3ee;padding:32px 16px;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="560" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e7e2d9;">
                            <tr>
                              <td style="background:#1f4d46;padding:22px 30px;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:-0.02em;">
                                SkillSwap
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:30px;">
                                <p style="margin:0 0 12px;font-size:15px;color:#52606d;">Hi %s,</p>
                                <h1 style="margin:0 0 14px;font-size:22px;line-height:1.3;color:#16241f;">%s</h1>
                                <p style="margin:0 0 24px;font-size:15px;line-height:1.65;color:#3f4a45;">%s</p>
                                <a href="%s" style="display:inline-block;background:#1f4d46;color:#ffffff;text-decoration:none;font-weight:600;font-size:15px;padding:12px 22px;border-radius:10px;">
                                  Open SkillSwap
                                </a>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:18px 30px;border-top:1px solid #ece7de;font-size:12px;line-height:1.5;color:#7b8580;">
                                You are receiving this because you have a SkillSwap account.
                                In-app notifications remain available even if email delivery fails.
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(safeName, safeTitle, safeBody, safeUrl);
    }

    public record TestResult(boolean sent, String message) {}
}
