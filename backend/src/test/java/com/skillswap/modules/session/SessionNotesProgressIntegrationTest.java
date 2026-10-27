package com.skillswap.modules.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillswap.modules.learning.repository.LearningRequestRepository;
import com.skillswap.modules.session.repository.SessionNoteRepository;
import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.entity.UserSkill;
import com.skillswap.modules.skill.repository.SkillRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.swap.repository.SkillSwapRepository;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Session notes are shared between the two participants, unlock once the
 * session is accepted, and feed the dashboard's progress summary.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionNotesProgressIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private SkillSwapRepository swapRepository;
    @Autowired private LearningRequestRepository learningRepository;
    @Autowired private SessionNoteRepository sessionNoteRepository;
    @Autowired private com.skillswap.modules.review.repository.ReviewRepository reviewRepository;
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
    private Long reactId;

    @BeforeEach
    void setUp() throws Exception {
        sessionNoteRepository.deleteAll();
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

        teacherToken = registerAndGetToken("rita_notes", "rita_notes@example.com", "Rita Teacher");
        learnerToken = registerAndGetToken("sam_notes", "sam_notes@example.com", "Sam Learner");

        teacherId = userRepository.findByUsername("rita_notes").orElseThrow().getId();
        learnerId = userRepository.findByUsername("sam_notes").orElseThrow().getId();

        addUserSkill(teacherId, pythonId, "TEACH");
        addUserSkill(teacherId, reactId, "LEARN");
        addUserSkill(learnerId, reactId, "TEACH");
        addUserSkill(learnerId, pythonId, "LEARN");
    }

    @AfterEach
    void tearDown() {
        sessionNoteRepository.deleteAll();
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

    private Long createPendingLearningSession() throws Exception {
        String payload = """
                {"teacherId": %d, "skillId": %d}
                """.formatted(teacherId, pythonId);

        String body = mockMvc.perform(post("/api/learning")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).at("/data/id").asLong();
    }

    private Long createCompletedLearningSession() throws Exception {
        Long id = createPendingLearningSession();

        mockMvc.perform(put("/api/learning/" + id + "/accept")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/learning/" + id + "/complete")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk());

        return id;
    }

    private Long createCompletedSwap() throws Exception {
        String payload = """
                {"receiverId": %d, "offeredSkillId": %d, "requestedSkillId": %d,
                 "message": "Let's trade"}
                """.formatted(teacherId, reactId, pythonId);

        String body = mockMvc.perform(post("/api/swaps")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).at("/data/id").asLong();

        mockMvc.perform(put("/api/swaps/" + id + "/accept")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/swaps/" + id + "/complete")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        return id;
    }

    private String addNote(String token, String type, Long sessionId, String content) throws Exception {
        String payload = """
                {"title": "Week 1", "content": "%s", "nextStep": "Practice one small example"}
                """.formatted(content);

        return mockMvc.perform(post("/api/sessions/" + type + "/" + sessionId + "/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("Participants can add, read, edit, and delete a shared note")
    void notesLifecycle() throws Exception {
        Long sessionId = createCompletedLearningSession();

        String body = addNote(learnerToken, "learning", sessionId, "Covered lists and loops");
        Long noteId = objectMapper.readTree(body).at("/data/id").asLong();

        mockMvc.perform(get("/api/sessions/learning/" + sessionId + "/notes")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Week 1"))
                .andExpect(jsonPath("$.data[0].content").value("Covered lists and loops"))
                .andExpect(jsonPath("$.data[0].nextStep").value("Practice one small example"))
                .andExpect(jsonPath("$.data[0].author.username").value("sam_notes"));

        mockMvc.perform(put("/api/sessions/learning/" + sessionId + "/notes/" + noteId)
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Week 1 recap", "content": "Lists, loops, and a quiz"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Week 1 recap"))
                .andExpect(jsonPath("$.data.nextStep").doesNotExist());

        mockMvc.perform(delete("/api/sessions/learning/" + sessionId + "/notes/" + noteId)
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sessions/learning/" + sessionId + "/notes")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Notes stay locked until acceptance and are hidden from outsiders")
    void notesAuthorization() throws Exception {
        Long pendingId = createPendingLearningSession();

        mockMvc.perform(post("/api/sessions/learning/" + pendingId + "/notes")
                        .header("Authorization", "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Too early"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/learning/" + pendingId + "/accept")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        String outsiderToken = registerAndGetToken(
                "otto_notes", "otto_notes@example.com", "Otto Outsider");

        mockMvc.perform(post("/api/sessions/learning/" + pendingId + "/notes")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Not my session"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/sessions/learning/" + pendingId + "/notes")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Only the author can edit or delete their note")
    void onlyAuthorCanChangeNote() throws Exception {
        Long sessionId = createCompletedLearningSession();
        String body = addNote(learnerToken, "learning", sessionId, "My private draft");
        Long noteId = objectMapper.readTree(body).at("/data/id").asLong();

        mockMvc.perform(put("/api/sessions/learning/" + sessionId + "/notes/" + noteId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Editing someone else's note"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/sessions/learning/" + sessionId + "/notes/" + noteId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Swap sessions expose the same shared-notes API")
    void notesWorkForSwaps() throws Exception {
        Long swapId = createCompletedSwap();

        addNote(teacherToken, "swap", swapId, "Swapped Python basics for React hooks");

        mockMvc.perform(get("/api/sessions/swap/" + swapId + "/notes")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionType").value("SWAP"))
                .andExpect(jsonPath("$.data[0].sessionId").value(swapId))
                .andExpect(jsonPath("$.data[0].author.username").value("rita_notes"));
    }

    @Test
    @DisplayName("Progress counts completed sessions, notes, and zero-session learning goals")
    void progressReflectsCompletedActivity() throws Exception {
        Long learningId = createCompletedLearningSession();
        createCompletedSwap();
        addNote(learnerToken, "learning", learningId, "Covered functions today");

        mockMvc.perform(get("/api/progress/me")
                        .header("Authorization", "Bearer " + learnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningSessions").value(2))
                .andExpect(jsonPath("$.data.teachingSessions").value(1))
                .andExpect(jsonPath("$.data.skillsInProgress").value(1))
                .andExpect(jsonPath("$.data.notesWritten").value(1))
                .andExpect(jsonPath("$.data.skills[0].skillName").value("Python"))
                .andExpect(jsonPath("$.data.skills[0].completedSessions").value(2))
                .andExpect(jsonPath("$.data.skills[0].targetSessions").value(5))
                .andExpect(jsonPath("$.data.skills[0].stage").value("Getting started"))
                .andExpect(jsonPath("$.data.skills[0].latestSessionId").isNumber());

        mockMvc.perform(get("/api/progress/me")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learningSessions").value(1))
                .andExpect(jsonPath("$.data.teachingSessions").value(2))
                .andExpect(jsonPath("$.data.skills[0].skillName").value("React"))
                .andExpect(jsonPath("$.data.skills[0].completedSessions").value(1));
    }
}
