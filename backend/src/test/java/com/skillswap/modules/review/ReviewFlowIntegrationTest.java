package com.skillswap.modules.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillswap.modules.learning.repository.LearningRequestRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reviews may only be left for completed interactions, by participants,
 * once each. Ratings then aggregate onto the public profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private SkillSwapRepository swapRepository;
    @Autowired private LearningRequestRepository learningRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private com.skillswap.modules.problem.repository.AnswerRepository answerRepository;
    @Autowired private com.skillswap.modules.problem.repository.ProblemRepository problemRepository;
    @Autowired private com.skillswap.modules.chat.repository.MessageRepository messageRepository;
    @Autowired private com.skillswap.modules.chat.repository.ConversationRepository conversationRepository;
    @Autowired private com.skillswap.modules.notification.repository.NotificationRepository notificationRepository;

    private String teacherToken;
    private String learnerToken;
    private Long teacherId;
    private Long learnerId;
    private Long pythonId;

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

        teacherToken = registerAndGetToken("rita_review", "rita_review@example.com", "Rita Reviewer");
        learnerToken = registerAndGetToken("sam_review", "sam_review@example.com", "Sam Student");

        teacherId = userRepository.findByUsername("rita_review").orElseThrow().getId();
        learnerId = userRepository.findByUsername("sam_review").orElseThrow().getId();

        addUserSkill(teacherId, pythonId, "TEACH");
        addUserSkill(learnerId, pythonId, "TEACH");
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

    /** Creates a learning request and drives it to COMPLETED. */
    private Long completedSession() throws Exception {
        String payload = """
                {"teacherId": %d, "skillId": %d}
                """.formatted(teacherId, pythonId);

        String body = mockMvc.perform(post("/api/learning")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).at("/data/id").asLong();

        mockMvc.perform(put("/api/learning/" + id + "/accept")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/learning/" + id + "/complete")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk());

        return id;
    }

    @Test
    @DisplayName("A participant can review a completed session, and it aggregates")
    void reviewCompletedSession() throws Exception {
        Long sessionId = completedSession();

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learningRequestId": %d, "rating": 5, "comment": "Excellent!"}
                                """.formatted(sessionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.revieweeId").value(teacherId))
                .andExpect(jsonPath("$.data.contextType").value("LEARNING"))
                .andExpect(jsonPath("$.data.skillName").value("Python"));

        mockMvc.perform(get("/api/users/" + teacherId + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avgRating").value(5.0))
                .andExpect(jsonPath("$.data.reviewCount").value(1));
    }

    @Test
    @DisplayName("An uncompleted session cannot be reviewed")
    void cannotReviewPendingSession() throws Exception {
        String payload = """
                {"teacherId": %d, "skillId": %d}
                """.formatted(teacherId, pythonId);

        String body = mockMvc.perform(post("/api/learning")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).at("/data/id").asLong();

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learningRequestId": %d, "rating": 5}
                                """.formatted(id)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("The same session cannot be reviewed twice")
    void duplicateReviewRejected() throws Exception {
        Long sessionId = completedSession();

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learningRequestId": %d, "rating": 4}
                                """.formatted(sessionId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learningRequestId": %d, "rating": 1}
                                """.formatted(sessionId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A non-participant cannot review the session")
    void outsiderCannotReview() throws Exception {
        Long sessionId = completedSession();
        String outsiderToken = registerAndGetToken(
                "otto_review", "otto_review@example.com", "Otto Outsider");

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learningRequestId": %d, "rating": 5}
                                """.formatted(sessionId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ratings outside 1-5 are rejected")
    void ratingMustBeInRange() throws Exception {
        Long sessionId = completedSession();

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learningRequestId": %d, "rating": 6}
                                """.formatted(sessionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Both participants see the other in their pending-review list")
    void pendingReviewListIsSymmetric() throws Exception {
        Long sessionId = completedSession();

        mockMvc.perform(get("/api/reviews/pending")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].contextId").value(sessionId))
                .andExpect(jsonPath("$.data[0].counterpartName").value("Sam Student"));

        mockMvc.perform(get("/api/reviews/pending")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].counterpartName").value("Rita Reviewer"));
    }

    @Test
    @DisplayName("Reviews are publicly readable and hide the reviewer's email")
    void reviewsArePublicAndSafe() throws Exception {
        Long sessionId = completedSession();

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"learningRequestId": %d, "rating": 5, "comment": "Great"}
                                """.formatted(sessionId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/user/" + teacherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rating").value(5))
                .andExpect(jsonPath("$.data[0].reviewer.email").doesNotExist());
    }
}
