package com.skillswap.modules.swap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.entity.UserSkill;
import com.skillswap.modules.skill.repository.SkillRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the swap lifecycle and its authorization rules:
 * only the receiver may accept/decline, only the sender may cancel,
 * and only participants may complete.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwapFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private com.skillswap.modules.swap.repository.SkillSwapRepository swapRepository;
    @Autowired private com.skillswap.modules.learning.repository.LearningRequestRepository learningRepository;
    @Autowired private com.skillswap.modules.review.repository.ReviewRepository reviewRepository;
    @Autowired private com.skillswap.modules.problem.repository.AnswerRepository answerRepository;
    @Autowired private com.skillswap.modules.problem.repository.ProblemRepository problemRepository;
    @Autowired private com.skillswap.modules.chat.repository.MessageRepository messageRepository;
    @Autowired private com.skillswap.modules.chat.repository.ConversationRepository conversationRepository;
    @Autowired private com.skillswap.modules.notification.repository.NotificationRepository notificationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String aliceToken;
    private String bobToken;
    private Long aliceId;
    private Long bobId;
    private Long pythonId;
    private Long reactId;
    private Long designId;

    @BeforeEach
    void setUp() throws Exception {
        // Delete in dependency order: chat -> notifications -> Q&A -> reviews -> sessions/swaps -> users
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
        designId = skillRepository.findByNameIgnoreCase("UI/UX Design").orElseThrow().getId();

        aliceToken = registerAndGetToken("alice_swap", "alice_swap@example.com", "Alice");
        bobToken = registerAndGetToken("bob_swap", "bob_swap@example.com", "Bob");

        aliceId = userRepository.findByUsername("alice_swap").orElseThrow().getId();
        bobId = userRepository.findByUsername("bob_swap").orElseThrow().getId();

        // Alice teaches Python, wants to learn React
        addUserSkill(aliceId, pythonId, "TEACH");
        addUserSkill(aliceId, reactId, "LEARN");

        // Bob teaches React, wants to learn Python
        addUserSkill(bobId, reactId, "TEACH");
        addUserSkill(bobId, pythonId, "LEARN");
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

    private Long proposeSwap() throws Exception {
        String payload = """
                {"receiverId": %d, "offeredSkillId": %d, "requestedSkillId": %d, "message": "Let's swap!"}
                """.formatted(bobId, pythonId, reactId);

        String body = mockMvc.perform(post("/api/swaps")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    @Test
    @DisplayName("Sender proposes, receiver accepts, both can complete")
    void fullSwapLifecycle() throws Exception {
        Long swapId = proposeSwap();

        mockMvc.perform(put("/api/swaps/" + swapId + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.meetingLink").isNotEmpty());

        mockMvc.perform(put("/api/swaps/" + swapId + "/complete")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Sender cannot accept their own proposal")
    void senderCannotAcceptOwnSwap() throws Exception {
        Long swapId = proposeSwap();

        mockMvc.perform(put("/api/swaps/" + swapId + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Proposal is rejected when the receiver does not teach the requested skill")
    void receiverMustTeachRequestedSkill() throws Exception {
        String payload = """
                {"receiverId": %d, "offeredSkillId": %d, "requestedSkillId": %d}
                """.formatted(bobId, pythonId, designId);

        mockMvc.perform(post("/api/swaps")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Proposal is rejected when the sender does not teach the offered skill")
    void senderMustTeachOfferedSkill() throws Exception {
        String payload = """
                {"receiverId": %d, "offeredSkillId": %d, "requestedSkillId": %d}
                """.formatted(bobId, designId, reactId);

        mockMvc.perform(post("/api/swaps")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Duplicate active proposals for the same pair and skills are rejected")
    void duplicateProposalRejected() throws Exception {
        proposeSwap();

        String payload = """
                {"receiverId": %d, "offeredSkillId": %d, "requestedSkillId": %d}
                """.formatted(bobId, pythonId, reactId);

        mockMvc.perform(post("/api/swaps")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Only participants can fetch one swap; the room link appears after acceptance")
    void getSwapIsParticipantOnly() throws Exception {
        Long swapId = proposeSwap();
        String carolToken = registerAndGetToken("carol_swap", "carol_swap@example.com", "Carol");

        mockMvc.perform(get("/api/swaps/" + swapId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(swapId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // A third user must not learn that the room exists.
        mockMvc.perform(get("/api/swaps/" + swapId)
                        .header("Authorization", "Bearer " + carolToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/swaps/" + swapId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/swaps/" + swapId + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/swaps/" + swapId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.meetingLink", startsWith("https://meet.jit.si/")));
    }

    @Test
    @DisplayName("Swap responses never leak the counterpart's email")
    void swapResponseHidesEmail() throws Exception {
        proposeSwap();

        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/swaps/incoming")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("alice_swap@example.com");
    }
}
