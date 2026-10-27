package com.skillswap.modules.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillswap.modules.chat.repository.ConversationRepository;
import com.skillswap.modules.chat.repository.MessageRepository;
import com.skillswap.modules.learning.repository.LearningRequestRepository;
import com.skillswap.modules.notification.repository.NotificationRepository;
import com.skillswap.modules.problem.repository.AnswerRepository;
import com.skillswap.modules.problem.repository.ProblemRepository;
import com.skillswap.modules.review.repository.ReviewRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.swap.repository.SkillSwapRepository;
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
 * One-to-one messaging: get-or-create threads, participant-only access,
 * unread accounting, newest-first inbox, and oldest-first message pages.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
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
    private String carolToken;
    private Long aliceId;
    private Long bobId;
    private Long carolId;

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

        aliceToken = register("alice_chat", "alice_chat@example.com", "Alice Chat");
        bobToken = register("bob_chat", "bob_chat@example.com", "Bob Chat");
        carolToken = register("carol_chat", "carol_chat@example.com", "Carol Chat");

        aliceId = userRepository.findByUsername("alice_chat").orElseThrow().getId();
        bobId = userRepository.findByUsername("bob_chat").orElseThrow().getId();
        carolId = userRepository.findByUsername("carol_chat").orElseThrow().getId();
    }

    @Test
    @DisplayName("Starting the same conversation twice returns the same thread")
    void startIsGetOrCreate() throws Exception {
        long first = start(aliceToken, bobId);
        long second = start(aliceToken, bobId);
        long fromOtherSide = start(bobToken, aliceId);

        assertThat(second).isEqualTo(first);
        assertThat(fromOtherSide).isEqualTo(first);
        assertThat(conversationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("You cannot start a conversation with yourself")
    void cannotMessageSelf() throws Exception {
        mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + aliceId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("A message is unread for the recipient and read for the sender")
    void sendMessageAndUnreadCounts() throws Exception {
        long conversationId = start(aliceToken, bobId);
        send(aliceToken, conversationId, "Hi Bob, ready to swap?");

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].counterpart.id").value(bobId))
                .andExpect(jsonPath("$.data[0].lastMessage").value("Hi Bob, ready to swap?"))
                .andExpect(jsonPath("$.data[0].unreadCount").value(0));

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].counterpart.id").value(aliceId))
                .andExpect(jsonPath("$.data[0].unreadCount").value(1));

        mockMvc.perform(get("/api/conversations/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        mockMvc.perform(get("/api/conversations/unread-count")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    @DisplayName("Opening a thread marks the other person's messages as read")
    void markReadClearsUnread() throws Exception {
        long conversationId = start(aliceToken, bobId);
        send(aliceToken, conversationId, "First");
        send(aliceToken, conversationId, "Second");

        mockMvc.perform(put("/api/conversations/" + conversationId + "/read")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(2));

        mockMvc.perform(get("/api/conversations/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].read").value(true))
                .andExpect(jsonPath("$.data[1].read").value(true));
    }

    @Test
    @DisplayName("A third person cannot read or write in someone else's thread")
    void outsiderIsRejected() throws Exception {
        long conversationId = start(aliceToken, bobId);
        send(aliceToken, conversationId, "Private");

        mockMvc.perform(get("/api/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + carolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Sneaking in\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/conversations/" + conversationId + "/read")
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Messages come back oldest-first and support paging backwards")
    void messagePaging() throws Exception {
        long conversationId = start(aliceToken, bobId);
        long first = send(aliceToken, conversationId, "one");
        long second = send(aliceToken, conversationId, "two");
        long third = send(aliceToken, conversationId, "three");

        mockMvc.perform(get("/api/conversations/" + conversationId + "/messages?size=2")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(second))
                .andExpect(jsonPath("$.data[1].id").value(third));

        mockMvc.perform(get("/api/conversations/" + conversationId
                        + "/messages?size=2&before=" + second)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(first));
    }

    @Test
    @DisplayName("Inbox is ordered by latest activity")
    void inboxOrderedByLatestMessage() throws Exception {
        long withBob = start(aliceToken, bobId);
        long withCarol = start(aliceToken, carolId);

        send(aliceToken, withBob, "to bob");
        send(aliceToken, withCarol, "to carol");

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(withCarol));

        send(aliceToken, withBob, "bob again");

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(withBob));
    }

    @Test
    @DisplayName("Blank messages are rejected and nothing is stored")
    void blankMessageRejected() throws Exception {
        long conversationId = start(aliceToken, bobId);

        mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"   \"}"))
                .andExpect(status().isBadRequest());

        assertThat(messageRepository.count()).isZero();
    }

    @Test
    @DisplayName("Messaging endpoints require a login")
    void endpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/conversations/unread-count"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1}"))
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

    private long start(String token, Long otherUserId) throws Exception {
        String body = mockMvc.perform(post("/api/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + otherUserId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.counterpart.id").value(otherUserId))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private long send(String token, long conversationId, String text) throws Exception {
        String body = mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("body", text))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.body").value(text))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }
}
