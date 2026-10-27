package com.skillswap.modules.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillswap.modules.chat.repository.ConversationRepository;
import com.skillswap.modules.chat.repository.MessageRepository;
import com.skillswap.modules.learning.repository.LearningRequestRepository;
import com.skillswap.modules.moderation.repository.ModerationActionRepository;
import com.skillswap.modules.moderation.repository.ReportRepository;
import com.skillswap.modules.notification.entity.NotificationType;
import com.skillswap.modules.notification.repository.NotificationRepository;
import com.skillswap.modules.problem.repository.AnswerRepository;
import com.skillswap.modules.problem.repository.ProblemRepository;
import com.skillswap.modules.review.repository.ReviewRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.swap.repository.SkillSwapRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.RoleRepository;
import com.skillswap.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reporting and moderation:
 * duplicate/self-report guards, the admin-only queue, hiding content with
 * reputation rollback, suspension enforcement on existing JWTs, and restore.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModerationFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private LearningRequestRepository learningRepository;
    @Autowired private SkillSwapRepository swapRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private ModerationActionRepository actionRepository;

    private String aliceToken;
    private String bobToken;
    private String carolToken;
    private String adminToken;

    private Long aliceId;
    private Long bobId;
    private Long carolId;
    private Long adminId;

    @BeforeEach
    void setUp() throws Exception {
        actionRepository.deleteAll();
        reportRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        notificationRepository.deleteAll();
        answerRepository.deleteAll();
        problemRepository.deleteAll();
        reviewRepository.deleteAll();
        learningRepository.deleteAll();
        swapRepository.deleteAll();
        userSkillRepository.deleteAll();
        userRepository.deleteAll();

        aliceToken = registerAndGetToken("alice_mod", "alice_mod@example.com", "Alice Asker");
        bobToken = registerAndGetToken("bob_mod", "bob_mod@example.com", "Bob Helper");
        carolToken = registerAndGetToken("carol_mod", "carol_mod@example.com", "Carol Reporter");
        adminToken = registerAndGetToken("admin_mod", "admin_mod@example.com", "Admin Moderator");

        aliceId = userRepository.findByUsername("alice_mod").orElseThrow().getId();
        bobId = userRepository.findByUsername("bob_mod").orElseThrow().getId();
        carolId = userRepository.findByUsername("carol_mod").orElseThrow().getId();
        adminId = userRepository.findByUsername("admin_mod").orElseThrow().getId();

        User admin = userRepository.findById(adminId).orElseThrow();
        admin.getRoles().add(roleRepository.findByName("ROLE_ADMIN").orElseThrow());
        userRepository.save(admin);
    }

    private String registerAndGetToken(String username, String email, String name) throws Exception {
        String payload = """
                {
                  "name": "%s",
                  "username": "%s",
                  "email": "%s",
                  "password": "secret123",
                  "college": "Test College",
                  "mode": "Both"
                }
                """.formatted(name, username, email);

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    private Long askQuestion(String token, String title) throws Exception {
        String body = mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s",
                                 "description": "A clear description of the problem.",
                                 "category": "Programming",
                                 "level": "BEGINNER"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private Long answerQuestion(String token, Long problemId) throws Exception {
        String body = mockMvc.perform(post("/api/problems/" + problemId + "/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Here is a helpful answer that explains the fix."}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private void acceptAnswer(String token, Long problemId, Long answerId) throws Exception {
        mockMvc.perform(put("/api/problems/" + problemId + "/answers/" + answerId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private Long report(String token, String targetType, Long targetId, String reason) throws Exception {
        String body = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "%s", "targetId": %d, "reason": "%s",
                                 "details": "This needs a moderator review."}
                                """.formatted(targetType, targetId, reason)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private void resolve(Long reportId, String action, String note) throws Exception {
        mockMvc.perform(put("/api/admin/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "%s", "note": "%s"}
                                """.formatted(action, note)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(
                        action.equals("DISMISS") ? "DISMISSED" : "RESOLVED"));
    }

    @Test
    @DisplayName("Reporting requires login, blocks self/duplicate reports, and the queue is admin-only")
    void reportingGuards() throws Exception {
        Long questionId = askQuestion(aliceToken, "Why is my loop skipping?");

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "PROBLEM", "targetId": %d, "reason": "SPAM"}
                                """.formatted(questionId)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "PROBLEM", "targetId": %d, "reason": "SPAM"}
                                """.formatted(questionId)))
                .andExpect(status().isBadRequest());

        Long reportId = report(carolToken, "PROBLEM", questionId, "SPAM");

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + carolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "PROBLEM", "targetId": %d, "reason": "HARASSMENT"}
                                """.formatted(questionId)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(reportId))
                .andExpect(jsonPath("$.data.items[0].targetPreview").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].reporterUsername").value("carol_mod"));
    }

    @Test
    @DisplayName("Hiding a reported question removes it from public reads and rolls back accepted-answer points")
    void hidingQuestionRollsBackReputation() throws Exception {
        Long questionId = askQuestion(aliceToken, "A reported question");
        Long answerId = answerQuestion(bobToken, questionId);
        acceptAnswer(aliceToken, questionId, answerId);

        assertThat(userRepository.findById(bobId).orElseThrow().getHelpfulPoints()).isEqualTo(10);

        Long reportId = report(carolToken, "PROBLEM", questionId, "HARASSMENT");

        mockMvc.perform(put("/api/admin/reports/" + reportId + "/claim")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.data.reviewedByName").value("Admin Moderator"));

        resolve(reportId, "HIDE_CONTENT", "Targeted harassment in the question body.");

        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        mockMvc.perform(get("/api/problems/" + questionId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/problems/" + questionId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hidden").value(true))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        assertThat(userRepository.findById(bobId).orElseThrow().getHelpfulPoints()).isZero();
        assertThat(notificationRepository.findAll().stream()
                .anyMatch(n -> n.getType() == NotificationType.CONTENT_HIDDEN
                        && n.getUser().getId().equals(aliceId))).isTrue();

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openReports").value(0))
                .andExpect(jsonPath("$.data.resolvedToday").value(1));
    }

    @Test
    @DisplayName("Hiding an accepted answer removes it and restore makes it visible again")
    void hidingAndRestoringAnswer() throws Exception {
        Long questionId = askQuestion(aliceToken, "A question with a reported answer");
        Long answerId = answerQuestion(bobToken, questionId);
        acceptAnswer(aliceToken, questionId, answerId);

        Long reportId = report(carolToken, "ANSWER", answerId, "MISINFORMATION");
        resolve(reportId, "HIDE_CONTENT", "The answer gives unsafe advice.");

        mockMvc.perform(get("/api/problems/" + questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.answers").isEmpty());

        assertThat(userRepository.findById(bobId).orElseThrow().getHelpfulPoints()).isZero();

        mockMvc.perform(put("/api/admin/answers/" + answerId + "/restore")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note": "Appeal accepted; the answer was restored."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/problems/" + questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answers.length()").value(1))
                .andExpect(jsonPath("$.data.answers[0].accepted").value(false));

        assertThat(userRepository.findById(bobId).orElseThrow().getHelpfulPoints()).isZero();
    }

    @Test
    @DisplayName("Suspending a reported user blocks login and existing JWTs until reactivation")
    void suspensionBlocksAccess() throws Exception {
        Long reportId = report(carolToken, "USER", bobId, "HARASSMENT");
        resolve(reportId, "SUSPEND_USER", "Repeated harassment after a warning.");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"bob_mod","password":"secret123"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/users/" + bobId + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note": "Appeal accepted."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usernameOrEmail":"bob_mod","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("The queue puts high-risk reports ahead of low-risk ones")
    void queueOrdersByRisk() throws Exception {
        Long lowQuestion = askQuestion(aliceToken, "A low-risk reported question");
        Long highQuestion = askQuestion(aliceToken, "A high-risk reported question");

        report(carolToken, "PROBLEM", lowQuestion, "SPAM");
        report(carolToken, "PROBLEM", highQuestion, "HARASSMENT");

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].reason").value("HARASSMENT"))
                .andExpect(jsonPath("$.data.items[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.data.items[1].reason").value("SPAM"))
                .andExpect(jsonPath("$.data.items[1].priority").value("LOW"));
    }

    @Test
    @DisplayName("Moderator accounts cannot be suspended through a report")
    void adminsCannotBeSuspended() throws Exception {
        Long adminQuestion = askQuestion(adminToken, "A moderator's own question");
        Long reportId = report(carolToken, "PROBLEM", adminQuestion, "OTHER");

        mockMvc.perform(put("/api/admin/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "SUSPEND_USER", "note": "Trying to suspend a moderator."}
                                """))
                .andExpect(status().isBadRequest());
    }
}
