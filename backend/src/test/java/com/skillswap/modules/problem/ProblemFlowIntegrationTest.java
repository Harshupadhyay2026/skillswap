package com.skillswap.modules.problem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillswap.modules.problem.repository.AnswerRepository;
import com.skillswap.modules.problem.repository.ProblemRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
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
 * Q&A rules: anyone may ask or answer, only the asker may accept, one accepted
 * answer per question, and accepting awards reputation to the answerer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProblemFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private com.skillswap.modules.review.repository.ReviewRepository reviewRepository;
    @Autowired private com.skillswap.modules.learning.repository.LearningRequestRepository learningRepository;
    @Autowired private com.skillswap.modules.swap.repository.SkillSwapRepository swapRepository;
    @Autowired private com.skillswap.modules.chat.repository.MessageRepository messageRepository;
    @Autowired private com.skillswap.modules.chat.repository.ConversationRepository conversationRepository;
    @Autowired private com.skillswap.modules.notification.repository.NotificationRepository notificationRepository;

    private String askerToken;
    private String helperToken;
    private Long askerId;
    private Long helperId;

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

        askerToken = registerAndGetToken("ayana_qa", "ayana_qa@example.com", "Ayana Asker");
        helperToken = registerAndGetToken("dev_qa", "dev_qa@example.com", "Dev Helper");

        askerId = userRepository.findByUsername("ayana_qa").orElseThrow().getId();
        helperId = userRepository.findByUsername("dev_qa").orElseThrow().getId();
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

    private Long askQuestion(String token) throws Exception {
        String body = mockMvc.perform(post("/api/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Why does my loop skip an item?",
                                 "description": "I am iterating a list and removing inside the loop.",
                                 "category": "Programming",
                                 "level": "BEGINNER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private Long answerQuestion(String token, Long problemId) throws Exception {
        String body = mockMvc.perform(post("/api/problems/" + problemId + "/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Do not mutate the list while iterating; build a new list instead."}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    @Test
    @DisplayName("Questions are public to read but require login to post")
    void readingIsPublicWritingIsNot() throws Exception {
        Long id = askQuestion(askerToken);

        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(id))
                .andExpect(jsonPath("$.data.items[0].title").value("Why does my loop skip an item?"))
                .andExpect(jsonPath("$.data.items[0].author.username").value("ayana_qa"));

        mockMvc.perform(get("/api/problems/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerCount").doesNotExist())
                .andExpect(jsonPath("$.data.answers").isArray());

        mockMvc.perform(post("/api/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Anonymous", "description": "x", "category": "Programming"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Questions can be filtered by status")
    void listFiltersByStatus() throws Exception {
        Long id = askQuestion(askerToken);
        Long answerId = answerQuestion(helperToken, id);

        mockMvc.perform(put("/api/problems/" + id + "/answers/" + answerId + "/accept")
                        .header("Authorization", "Bearer " + askerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/problems").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        mockMvc.perform(get("/api/problems").param("status", "SOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("SOLVED"))
                .andExpect(jsonPath("$.data.items[0].answerCount").value(1));
    }

    @Test
    @DisplayName("Accepting an answer marks the question solved and awards reputation")
    void acceptingAwardsReputation() throws Exception {
        Long problemId = askQuestion(askerToken);
        Long answerId = answerQuestion(helperToken, problemId);

        mockMvc.perform(put("/api/problems/" + problemId + "/answers/" + answerId + "/accept")
                        .header("Authorization", "Bearer " + askerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SOLVED"))
                .andExpect(jsonPath("$.data.answers[0].accepted").value(true));

        mockMvc.perform(get("/api/users/" + helperId + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.helpfulPoints").value(10))
                .andExpect(jsonPath("$.data.answersGiven").value(1))
                .andExpect(jsonPath("$.data.acceptedAnswers").value(1));

        mockMvc.perform(get("/api/users/" + askerId + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionsAsked").value(1))
                .andExpect(jsonPath("$.data.helpfulPoints").value(0));
    }

    @Test
    @DisplayName("Only the asker can accept an answer")
    void onlyAskerCanAccept() throws Exception {
        Long problemId = askQuestion(askerToken);
        Long answerId = answerQuestion(helperToken, problemId);

        mockMvc.perform(put("/api/problems/" + problemId + "/answers/" + answerId + "/accept")
                        .header("Authorization", "Bearer " + helperToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("The asker cannot answer their own question")
    void cannotAnswerOwnQuestion() throws Exception {
        Long problemId = askQuestion(askerToken);

        mockMvc.perform(post("/api/problems/" + problemId + "/answers")
                        .header("Authorization", "Bearer " + askerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Talking to myself."}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("One user cannot post two answers to the same question")
    void duplicateAnswerRejected() throws Exception {
        Long problemId = askQuestion(askerToken);
        answerQuestion(helperToken, problemId);

        mockMvc.perform(post("/api/problems/" + problemId + "/answers")
                        .header("Authorization", "Bearer " + helperToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "A second, conflicting answer."}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Only one answer can be accepted per question")
    void onlyOneAcceptedAnswer() throws Exception {
        Long problemId = askQuestion(askerToken);
        Long first = answerQuestion(helperToken, problemId);

        String secondHelperToken = registerAndGetToken(
                "nia_qa", "nia_qa@example.com", "Nia Helper");

        String body = mockMvc.perform(post("/api/problems/" + problemId + "/answers")
                        .header("Authorization", "Bearer " + secondHelperToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Another valid approach."}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long second = objectMapper.readTree(body).at("/data/id").asLong();

        mockMvc.perform(put("/api/problems/" + problemId + "/answers/" + first + "/accept")
                        .header("Authorization", "Bearer " + askerToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/problems/" + problemId + "/answers/" + second + "/accept")
                        .header("Authorization", "Bearer " + askerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A closed question accepts no new answers but can be reopened")
    void closedQuestionRules() throws Exception {
        Long problemId = askQuestion(askerToken);

        mockMvc.perform(put("/api/problems/" + problemId + "/close")
                        .header("Authorization", "Bearer " + askerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(post("/api/problems/" + problemId + "/answers")
                        .header("Authorization", "Bearer " + helperToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Too late?"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/problems/" + problemId + "/reopen")
                        .header("Authorization", "Bearer " + askerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        answerQuestion(helperToken, problemId);
    }

    @Test
    @DisplayName("My questions lists only the asker's own posts and requires login")
    void myQuestionsIsScoped() throws Exception {
        askQuestion(askerToken);
        askQuestion(helperToken);

        mockMvc.perform(get("/api/problems/mine"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/problems/mine")
                        .header("Authorization", "Bearer " + askerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].author.username").value("ayana_qa"));
    }
}
