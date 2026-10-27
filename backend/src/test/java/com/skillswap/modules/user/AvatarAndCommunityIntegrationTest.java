package com.skillswap.modules.user;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Avatar upload (validation, public serving, replacement, removal) and the
 * public contributor leaderboard (ranking, counts, limit, empty state).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvatarAndCommunityIntegrationTest {

    private static final byte[] PNG_BYTES = pngBytes();

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

        aliceToken = register("alice_avatar", "alice_avatar@example.com", "Alice Avatar");
        bobToken = register("bob_avatar", "bob_avatar@example.com", "Bob Helper");
        carolToken = register("carol_avatar", "carol_avatar@example.com", "Carol Helper");

        aliceId = userRepository.findByUsername("alice_avatar").orElseThrow().getId();
        bobId = userRepository.findByUsername("bob_avatar").orElseThrow().getId();
    }

    @Test
    @DisplayName("A PNG avatar is stored and served publicly")
    void uploadAvatarAndServeIt() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "me.png", "image/png", PNG_BYTES);

        String body = mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl", startsWith("/api/files/avatars/")))
                .andReturn().getResponse().getContentAsString();

        String avatarUrl = objectMapper.readTree(body).at("/data/avatarUrl").asText();

        // Served without a login so <img> tags work for logged-out visitors.
        mockMvc.perform(get(avatarUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @DisplayName("A non-image upload is rejected")
    void rejectsNonImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.png", "image/png", "not really an image".getBytes());

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Removing the avatar clears the profile field")
    void removeAvatar() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "me.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/api/users/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/me/avatar")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").value(nullValue()));
    }

    @Test
    @DisplayName("Avatar upload requires a login")
    void uploadRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "me.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/api/users/me/avatar").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Leaderboard ranks accepted answers and is public")
    void leaderboardRanksContributors() throws Exception {
        long questionOne = askQuestion(aliceToken, "Question one");
        long bobAnswerOne = answerQuestion(bobToken, questionOne, "Answer one");

        long questionTwo = askQuestion(aliceToken, "Question two");
        long bobAnswerTwo = answerQuestion(bobToken, questionTwo, "Answer two");

        long questionThree = askQuestion(aliceToken, "Question three");
        long carolAnswer = answerQuestion(carolToken, questionThree, "Carol's answer");

        accept(aliceToken, questionOne, bobAnswerOne);
        accept(aliceToken, questionTwo, bobAnswerTwo);
        accept(aliceToken, questionThree, carolAnswer);

        // No Authorization header: the leaderboard is public.
        mockMvc.perform(get("/api/community/top-contributors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(bobId))
                .andExpect(jsonPath("$.data[0].helpfulPoints").value(20))
                .andExpect(jsonPath("$.data[0].acceptedAnswers").value(2))
                .andExpect(jsonPath("$.data[0].answersGiven").value(2))
                .andExpect(jsonPath("$.data[1].rank").value(2))
                .andExpect(jsonPath("$.data[1].helpfulPoints").value(10));
    }

    @Test
    @DisplayName("Leaderboard limit is respected and an empty board is valid")
    void leaderboardLimitAndEmptyState() throws Exception {
        mockMvc.perform(get("/api/community/top-contributors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        long question = askQuestion(aliceToken, "Only question");
        long answer = answerQuestion(bobToken, question, "Only answer");
        accept(aliceToken, question, answer);

        mockMvc.perform(get("/api/community/top-contributors?limit=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
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

    private long askQuestion(String token, String title) throws Exception {
        String payload = """
                {"title": "%s",
                 "description": "How do I fix this?",
                 "category": "Programming",
                 "level": "BEGINNER"}
                """.formatted(title);

        String body = mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private long answerQuestion(String token, long problemId, String text) throws Exception {
        String body = mockMvc.perform(post("/api/problems/" + problemId + "/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("body", text))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private void accept(String token, long problemId, long answerId) throws Exception {
        mockMvc.perform(put("/api/problems/" + problemId + "/answers/" + answerId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    /** A minimal file that passes the magic-byte check (serving only needs the signature). */
    private static byte[] pngBytes() {
        byte[] bytes = new byte[128];
        bytes[0] = (byte) 0x89;
        bytes[1] = 0x50;
        bytes[2] = 0x4E;
        bytes[3] = 0x47;
        bytes[4] = 0x0D;
        bytes[5] = 0x0A;
        bytes[6] = 0x1A;
        bytes[7] = 0x0A;
        Arrays.fill(bytes, 8, bytes.length, (byte) 0x00);
        return bytes;
    }
}
