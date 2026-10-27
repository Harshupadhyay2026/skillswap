package com.skillswap.modules.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillswap.modules.chat.repository.ConversationRepository;
import com.skillswap.modules.chat.repository.MessageRepository;
import com.skillswap.modules.learning.repository.LearningRequestRepository;
import com.skillswap.modules.notification.repository.NotificationRepository;
import com.skillswap.modules.problem.repository.AnswerRepository;
import com.skillswap.modules.problem.repository.ProblemRepository;
import com.skillswap.modules.review.repository.ReviewRepository;
import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.entity.UserSkill;
import com.skillswap.modules.skill.repository.SkillRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.swap.repository.SkillSwapRepository;
import com.skillswap.modules.user.entity.User;
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
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notifications are created by the action that affects someone else, are
 * readable only by their owner, and drive the navbar unread badge.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private SkillSwapRepository swapRepository;
    @Autowired private LearningRequestRepository learningRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private NotificationRepository notificationRepository;

    private String aliceToken;
    private String bobToken;
    private Long aliceId;
    private Long bobId;
    private Long pythonId;
    private Long reactId;

    @BeforeEach
    void setUp() throws Exception {
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

        pythonId = skillRepository.findByNameIgnoreCase("Python").orElseThrow().getId();
        reactId = skillRepository.findByNameIgnoreCase("React").orElseThrow().getId();

        aliceToken = register("alice_notify", "alice_notify@example.com", "Alice Notify");
        bobToken = register("bob_notify", "bob_notify@example.com", "Bob Notify");

        aliceId = userRepository.findByUsername("alice_notify").orElseThrow().getId();
        bobId = userRepository.findByUsername("bob_notify").orElseThrow().getId();

        addUserSkill(aliceId, pythonId, "TEACH");
        addUserSkill(bobId, reactId, "TEACH");
    }

    @Test
    @DisplayName("A swap proposal notifies the receiver, not the sender")
    void swapProposalNotifiesReceiver() throws Exception {
        proposeSwap(aliceToken, bobId, pythonId, reactId);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].type").value("SWAP_PROPOSED"))
                .andExpect(jsonPath("$.data.items[0].link").value("/requests"))
                .andExpect(jsonPath("$.data.items[0].read").value(false));
    }

    @Test
    @DisplayName("Accepting a swap notifies the sender without re-notifying the actor")
    void swapAcceptanceNotifiesSender() throws Exception {
        long swapId = proposeSwap(aliceToken, bobId, pythonId, reactId);

        mockMvc.perform(put("/api/swaps/" + swapId + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("SWAP_ACCEPTED"));

        // Alice got the acceptance; Bob only has his original proposal.
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(jsonPath("$.data.count").value(1));
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("Declining a swap notifies the sender")
    void swapDeclineNotifiesSender() throws Exception {
        long swapId = proposeSwap(aliceToken, bobId, pythonId, reactId);

        mockMvc.perform(put("/api/swaps/" + swapId + "/decline")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("SWAP_DECLINED"));
    }

    @Test
    @DisplayName("A learning request notifies the teacher")
    void learningRequestNotifiesTeacher() throws Exception {
        createLearningRequest(bobToken, aliceId, pythonId);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("LEARNING_REQUESTED"))
                .andExpect(jsonPath("$.data.items[0].link").value("/requests"));
    }

    @Test
    @DisplayName("Accepting a learning request notifies the learner")
    void learningAcceptNotifiesLearner() throws Exception {
        long id = createLearningRequest(bobToken, aliceId, pythonId);

        mockMvc.perform(put("/api/learning/" + id + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("LEARNING_ACCEPTED"));
    }

    @Test
        @DisplayName("A learner without a teach skill can request a free shared-learning session")
        void learningRequestDoesNotRequireATeachSkill() throws Exception {
        String charlieToken = register("charlie_notify", "charlie_notify@example.com", "Charlie Notify");

        String payload = """
                {"teacherId": %d, "skillId": %d, "message": "Please teach me Python"}
                """.formatted(aliceId, pythonId);

        mockMvc.perform(post("/api/learning")
                        .header("Authorization", "Bearer " + charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skill.name").value("Python"));
    }

    @Test
    @DisplayName("Only participants can fetch one learning session; the room link appears after acceptance")
    void learningSessionIsParticipantOnly() throws Exception {
        long id = createLearningRequest(bobToken, aliceId, pythonId);
        String carolToken = register("carol_notify", "carol_notify@example.com", "Carol Notify");

        mockMvc.perform(get("/api/learning/" + id)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(get("/api/learning/" + id)
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/learning/" + id))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/learning/" + id + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/learning/" + id)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meetingLink", startsWith("https://meet.jit.si/")));
    }

    @Test
    @DisplayName("Accepting an answer notifies the answerer with a link to the question")
    void acceptedAnswerNotifiesAnswerer() throws Exception {
        long problemId = askQuestion(aliceToken);
        long answerId = answerQuestion(bobToken, problemId);

        mockMvc.perform(put("/api/problems/" + problemId + "/answers/" + answerId + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("ANSWER_ACCEPTED"))
                .andExpect(jsonPath("$.data.items[0].link").value("/qa/" + problemId));
    }

    @Test
    @DisplayName("Notifications can be marked individually or all at once")
    void markReadAndMarkAll() throws Exception {
        long swapId = proposeSwap(aliceToken, bobId, pythonId, reactId);

        long notificationId = latestNotificationId(bobToken);

        mockMvc.perform(put("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(jsonPath("$.data.count").value(0));

        // Accepting the existing proposal creates a notification for Alice,
        // which is then cleared by "mark all".
        mockMvc.perform(put("/api/swaps/" + swapId + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(1));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    @DisplayName("You cannot mark someone else's notification as read")
    void cannotReadSomeoneElsesNotification() throws Exception {
        proposeSwap(aliceToken, bobId, pythonId, reactId);
        long bobNotificationId = latestNotificationId(bobToken);

        mockMvc.perform(put("/api/notifications/" + bobNotificationId + "/read")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("Notification endpoints require a login")
    void notificationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────

    private String register(String username, String email, String name) throws Exception {
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

    private void addUserSkill(Long userId, Long skillId, String type) {
        User user = userRepository.findById(userId).orElseThrow();
        Skill skill = skillRepository.findById(skillId).orElseThrow();
        UserSkill us = new UserSkill();
        us.setUser(user);
        us.setSkill(skill);
        us.setSkillType(type);
        us.setProficiency("INTERMEDIATE");
        userSkillRepository.save(us);
    }

    private long proposeSwap(String token, Long receiverId, Long offeredSkillId, Long requestedSkillId)
            throws Exception {
        String payload = """
                {"receiverId": %d, "offeredSkillId": %d, "requestedSkillId": %d,
                 "message": "Let's swap"}
                """.formatted(receiverId, offeredSkillId, requestedSkillId);

        String body = mockMvc.perform(post("/api/swaps")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private long createLearningRequest(String token, Long teacherId, Long skillId) throws Exception {
        String payload = """
                {"teacherId": %d, "skillId": %d, "message": "Please teach me"}
                """.formatted(teacherId, skillId);

        String body = mockMvc.perform(post("/api/learning")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private long askQuestion(String token) throws Exception {
        String body = mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Why does my loop skip an item?",
                                 "description": "Removing inside a for-loop.",
                                 "category": "Programming",
                                 "level": "BEGINNER"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private long answerQuestion(String token, long problemId) throws Exception {
        String body = mockMvc.perform(post("/api/problems/" + problemId + "/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Iterate over a copy instead.\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private long latestNotificationId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/items/0/id").asLong();
    }
}
