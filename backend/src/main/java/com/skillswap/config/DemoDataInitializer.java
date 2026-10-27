package com.skillswap.config;

import com.skillswap.modules.problem.entity.Answer;
import com.skillswap.modules.problem.entity.Problem;
import com.skillswap.modules.problem.entity.ProblemStatus;
import com.skillswap.modules.problem.repository.AnswerRepository;
import com.skillswap.modules.problem.repository.ProblemRepository;
import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.entity.UserSkill;
import com.skillswap.modules.skill.repository.SkillRepository;
import com.skillswap.modules.skill.repository.UserSkillRepository;
import com.skillswap.modules.user.entity.Role;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.RoleRepository;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Seeds clearly labeled, repeat-safe local walkthrough data. */
@Component
@Order(2)
public class DemoDataInitializer implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "DemoLearn2026!";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProblemRepository problemRepository;
    private final AnswerRepository answerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-data.enabled:true}")
    private boolean enabled;

    public DemoDataInitializer(RoleRepository roleRepository,
                               UserRepository userRepository,
                               SkillRepository skillRepository,
                               UserSkillRepository userSkillRepository,
                               ProblemRepository problemRepository,
                               AnswerRepository answerRepository,
                               PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.problemRepository = problemRepository;
        this.answerRepository = answerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;

        List<User> profiles = seedProfiles();
        seedQuestionsAndAnswers(profiles);
    }

    private List<User> seedProfiles() {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        List<User> profiles = new ArrayList<>();

        for (int i = 0; i < PROFILE_DATA.size(); i++) {
            DemoProfile demo = PROFILE_DATA.get(i);
            String username = String.format("skillswap_demo_%02d", i + 1);
            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) {
                user = new User();
                user.setFullName(demo.name());
                user.setUsername(username);
                user.setEmail(String.format("demo%02d@skillswap.example", i + 1));
                user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
                user.setCollege(demo.college());
                user.setBranch(demo.branch());
                user.setYearOfStudy(demo.year());
                user.setCity(demo.city());
                user.setBio("DEMO PROFILE — " + demo.bio());
                user.setLearningMode("Both");
                user.getRoles().add(userRole);
                user = userRepository.save(user);
            }

            addSkillIfMissing(user, demo.teachSkill(), "TEACH", demo.teachLevel());
            addSkillIfMissing(user, demo.learnSkill(), "LEARN", demo.learnLevel());
            profiles.add(user);
        }
        return profiles;
    }

    private void addSkillIfMissing(User user, String skillName, String type, String proficiency) {
        Skill skill = skillRepository.findByNameIgnoreCase(skillName)
                .orElseThrow(() -> new IllegalStateException("Demo skill not seeded: " + skillName));
        if (userSkillRepository.findByUserIdAndSkillIdAndSkillType(user.getId(), skill.getId(), type).isPresent()) {
            return;
        }

        UserSkill userSkill = new UserSkill();
        userSkill.setUser(user);
        userSkill.setSkill(skill);
        userSkill.setSkillType(type);
        userSkill.setProficiency(proficiency);
        userSkillRepository.save(userSkill);
    }

    private void seedQuestionsAndAnswers(List<User> profiles) {
        for (int i = 0; i < QUESTION_DATA.size(); i++) {
            DemoQuestion demo = QUESTION_DATA.get(i);
            String title = "[Demo] " + demo.title();
            Problem problem = problemRepository.findByTitle(title).orElse(null);
            if (problem == null) {
                problem = new Problem();
                problem.setAuthor(profiles.get(i % profiles.size()));
                problem.setTitle(title);
                problem.setDescription("DEMO QUESTION — " + demo.description());
                problem.setCategory(demo.category());
                problem.setLevel(demo.level());
                problem.setStatus(ProblemStatus.OPEN);
                problem.setViews(12L + i * 7L);
                problem.setSkill(skillRepository.findByNameIgnoreCase(demo.skill())
                        .orElseThrow(() -> new IllegalStateException("Demo skill not seeded: " + demo.skill())));
                problem = problemRepository.save(problem);
            }

            // Two different demo members answer each question; the author never answers their own post.
            addAnswerIfMissing(problem, profiles.get((i + 1) % profiles.size()), ANSWER_TIPS.get(i % ANSWER_TIPS.size()));
            addAnswerIfMissing(problem, profiles.get((i + 2) % profiles.size()),
                    "Another approach is to build a tiny example first, test one change at a time, and write down what you observed. That makes it easier to spot where the idea is unclear.");
        }
    }

    private void addAnswerIfMissing(Problem problem, User author, String body) {
        if (answerRepository.existsByProblemIdAndAuthorId(problem.getId(), author.getId())) return;
        Answer answer = new Answer();
        answer.setProblem(problem);
        answer.setAuthor(author);
        answer.setBody("DEMO ANSWER — " + body);
        answerRepository.save(answer);
    }

    private record DemoProfile(String name, String college, String branch, String year, String city,
                               String bio, String teachSkill, String teachLevel,
                               String learnSkill, String learnLevel) {}

    private record DemoQuestion(String title, String description, String category,
                                String level, String skill) {}

    private static final List<DemoProfile> PROFILE_DATA = List.of(
            new DemoProfile("Aarav Sharma", "Northview University", "Computer Science", "2nd Year", "Delhi", "I enjoy explaining Python with small, practical examples.", "Python", "ADVANCED", "React", "BEGINNER"),
            new DemoProfile("Maya Patel", "Riverdale Institute", "Information Technology", "3rd Year", "Mumbai", "I build accessible interfaces and like learning through projects.", "React", "ADVANCED", "Python", "INTERMEDIATE"),
            new DemoProfile("Rohan Mehta", "Greenfield College", "Computer Science", "2nd Year", "Pune", "Happy to help with algorithms, debugging, and interview practice.", "DSA", "ADVANCED", "Java", "INTERMEDIATE"),
            new DemoProfile("Isha Kapoor", "Lakeside University", "Design", "1st Year", "Jaipur", "I make clean layouts and can share a beginner-friendly Figma workflow.", "Figma", "INTERMEDIATE", "HTML & CSS", "BEGINNER"),
            new DemoProfile("Kabir Rao", "Northview University", "Computer Science", "4th Year", "Bengaluru", "I like turning backend concepts into small working demos.", "Java", "ADVANCED", "Docker", "INTERMEDIATE"),
            new DemoProfile("Ananya Das", "City Tech Institute", "Data Science", "3rd Year", "Kolkata", "I teach SQL by practicing on realistic datasets.", "SQL", "ADVANCED", "Machine Learning", "INTERMEDIATE"),
            new DemoProfile("Dev Malhotra", "Riverdale Institute", "Computer Science", "2nd Year", "Chandigarh", "I enjoy helping people get comfortable with Git and collaboration.", "Git & GitHub", "ADVANCED", "Node.js", "BEGINNER"),
            new DemoProfile("Sara Khan", "Brighton College", "Computer Science", "3rd Year", "Hyderabad", "I can help you understand web fundamentals and responsive layouts.", "HTML & CSS", "ADVANCED", "TypeScript", "INTERMEDIATE"),
            new DemoProfile("Vihaan Joshi", "Lakeside University", "Information Technology", "2nd Year", "Ahmedabad", "I enjoy building APIs and sharing a step-by-step Spring Boot setup.", "Spring Boot", "INTERMEDIATE", "AWS", "BEGINNER"),
            new DemoProfile("Nisha Verma", "City Tech Institute", "Data Science", "4th Year", "Lucknow", "I make charts that tell a story and can help with data cleanup.", "Data Analysis", "ADVANCED", "Power BI", "INTERMEDIATE"),
            new DemoProfile("Arjun Sen", "Greenfield College", "Computer Science", "3rd Year", "Bhopal", "I like explaining how operating systems work under the hood.", "OS Concepts", "ADVANCED", "Networking", "INTERMEDIATE"),
            new DemoProfile("Tara Iyer", "Northview University", "Design", "2nd Year", "Chennai", "I help beginners turn rough ideas into clear user flows.", "UI/UX Design", "ADVANCED", "JavaScript", "INTERMEDIATE"),
            new DemoProfile("Neel Gupta", "Brighton College", "Computer Science", "1st Year", "Noida", "I am a patient peer mentor for C++ and problem-solving basics.", "C++", "INTERMEDIATE", "Python", "BEGINNER"),
            new DemoProfile("Zoya Ali", "Riverdale Institute", "Data Science", "3rd Year", "Kochi", "I enjoy exploring datasets and making dashboards useful.", "Power BI", "ADVANCED", "SQL", "INTERMEDIATE"),
            new DemoProfile("Aditya Nair", "Lakeside University", "Computer Science", "4th Year", "Thiruvananthapuram", "I can help with Linux command line and developer tools.", "Linux", "ADVANCED", "Docker", "INTERMEDIATE"),
            new DemoProfile("Meera Shah", "City Tech Institute", "Information Technology", "2nd Year", "Surat", "I teach JavaScript through small browser projects.", "JavaScript", "ADVANCED", "Vue.js", "INTERMEDIATE"),
            new DemoProfile("Reyansh Bose", "Greenfield College", "Computer Science", "3rd Year", "Indore", "I can explain database design and help debug queries.", "DBMS", "ADVANCED", "MongoDB", "INTERMEDIATE"),
            new DemoProfile("Diya Fernandes", "Brighton College", "Design", "1st Year", "Goa", "I enjoy making quick visual prototypes and sharing design feedback.", "Canva", "INTERMEDIATE", "UI/UX Design", "BEGINNER"),
            new DemoProfile("Yash Khanna", "Northview University", "Computer Science", "4th Year", "Gurugram", "I build small cloud demos and explain each deployment step.", "Docker", "ADVANCED", "Go", "INTERMEDIATE"),
            new DemoProfile("Aditi Roy", "Riverdale Institute", "Data Science", "2nd Year", "Nagpur", "I like breaking machine-learning ideas into approachable experiments.", "Machine Learning", "INTERMEDIATE", "Data Analysis", "BEGINNER"),
            new DemoProfile("Alex Demo", "SkillSwap Demo College", "Computer Science", "2nd Year", "Delhi", "Demo account for testing a Python and React skill swap.", "Python", "ADVANCED", "React", "INTERMEDIATE"),
            new DemoProfile("Blair Demo", "SkillSwap Demo College", "Information Technology", "3rd Year", "Mumbai", "Demo account for testing a React and Python skill swap.", "React", "ADVANCED", "Python", "INTERMEDIATE")
    );

    private static final List<DemoQuestion> QUESTION_DATA = List.of(
            new DemoQuestion("How should I start learning Python functions?", "I know variables and loops. What is a good first exercise for understanding functions and parameters?", "Programming", "BEGINNER", "Python"),
            new DemoQuestion("Why does my React component render more than once?", "I am learning state and effects. How can I tell whether a rerender is expected or caused by a bug?", "Web Dev", "BEGINNER", "React"),
            new DemoQuestion("How do I choose the right data structure for a problem?", "I can solve basic array problems but get confused between a map, set, stack, and queue.", "CS Fundamentals", "INTERMEDIATE", "DSA"),
            new DemoQuestion("What makes a Figma prototype useful for feedback?", "I have designed a few screens. Should I prototype every interaction or just the main user journey?", "Design", "BEGINNER", "Figma"),
            new DemoQuestion("How should I structure a small Java project?", "I have one long class and want to split it sensibly without overengineering.", "Programming", "INTERMEDIATE", "Java"),
            new DemoQuestion("What is a practical way to practice SQL joins?", "I understand SELECT statements but INNER JOIN and LEFT JOIN still feel abstract.", "Data & AI", "BEGINNER", "SQL"),
            new DemoQuestion("How do branches help when working with Git?", "I am working on a group project. What branch workflow is simple and safe for beginners?", "DevOps", "BEGINNER", "Git & GitHub"),
            new DemoQuestion("How can I make a page responsive without guessing?", "My layout looks fine on my laptop but breaks on a phone. How should I debug it?", "Web Dev", "INTERMEDIATE", "HTML & CSS"),
            new DemoQuestion("What should I learn before deploying a Spring Boot API?", "I can run an API locally. Which configuration and security basics should I understand next?", "Web Dev", "INTERMEDIATE", "Spring Boot"),
            new DemoQuestion("How do I clean a dataset with missing values?", "I have a CSV with blanks and inconsistent categories. What should I inspect before filling anything?", "Data & AI", "INTERMEDIATE", "Data Analysis"),
            new DemoQuestion("Can someone explain processes versus threads simply?", "I am studying operating systems and would like a practical analogy and a small example.", "CS Fundamentals", "BEGINNER", "OS Concepts"),
            new DemoQuestion("How do I test whether a user flow is easy to understand?", "I have designed a sign-up flow. What can I ask a classmate to test without leading them?", "Design", "INTERMEDIATE", "UI/UX Design"),
            new DemoQuestion("What is the difference between a vector and a linked list?", "I know both store values, but I am unsure how their memory and access behavior differ.", "Programming", "BEGINNER", "C++"),
            new DemoQuestion("What is a good first dashboard project in Power BI?", "I want to practice charts, filters, and a clear summary using a dataset that is not too large.", "Data & AI", "BEGINNER", "Power BI"),
            new DemoQuestion("Which Linux commands should a student know first?", "I can list files and change directories. What commands are useful for everyday development?", "DevOps", "BEGINNER", "Linux"),
            new DemoQuestion("When should I use TypeScript in a small project?", "I know JavaScript basics. How can types help without slowing down a beginner project?", "Programming", "INTERMEDIATE", "TypeScript"),
            new DemoQuestion("How do I design tables for a simple club app?", "The app has members, events, and registrations. How should I avoid duplicate data?", "CS Fundamentals", "INTERMEDIATE", "DBMS"),
            new DemoQuestion("How can I choose a color palette that stays readable?", "I am making a student project and want it to look polished while remaining accessible.", "Design", "BEGINNER", "Canva"),
            new DemoQuestion("What is the safest way to learn Docker locally?", "I want to understand images and containers without changing my computer setup too much.", "DevOps", "BEGINNER", "Docker"),
            new DemoQuestion("How do I evaluate a simple machine-learning model?", "I trained a classifier and got an accuracy score. What else should I check before trusting it?", "Data & AI", "INTERMEDIATE", "Machine Learning")
    );

    private static final List<String> ANSWER_TIPS = List.of(
            "Start with a function that turns an input into an output, such as converting Celsius to Fahrenheit. Then add one parameter and print or return the result. Small examples make scope and return values easier to see.",
            "First check whether state actually changed and whether Strict Mode is enabled in development. Add a small console log, then inspect effects and dependencies; avoid adding an effect when the value can be calculated during render.",
            "Write down the operations you need: fast lookup suggests a map or set, last-in-first-out suggests a stack, and first-in-first-out suggests a queue. Compare the operation requirements before choosing.",
            "Prototype the main task from start to finish, including one or two important decisions. Ask a peer to try it without instructions and note where they hesitate; that is more useful than animating every detail.",
            "Separate code by responsibility: keep the entry point small, move domain logic into focused classes, and use methods with clear names. Add another abstraction only when it makes a real change easier to understand or test.",
            "Create two tiny tables with repeated values and write one question in plain language, such as 'show every student and their club if they joined one'. Try LEFT JOIN first, then compare it with INNER JOIN to see which rows disappear.",
            "Create a branch for one task, commit small changes, and open a pull request for review. Pull the latest main branch before starting a new task and avoid committing generated files or secrets.",
            "Use browser dev tools at a narrow viewport, inspect the element that overflows, and check fixed widths and long text first. Prefer flexible grids, max-widths, and a small number of intentional breakpoints.",
            "Keep secrets in environment variables, validate inputs, use clear error responses, and test the API locally before deployment. Start with a non-sensitive demo endpoint and read the framework's production configuration guidance.",
            "Inspect how many values are missing and whether the missingness has a pattern. Keep the original data, document any fill/drop choice, and compare results before and after cleaning so the change is explainable.",
            "A process is an independent running program with its own resources; threads are execution paths inside a process that can share memory. A text editor with multiple workers is a useful analogy, then look at a small program creating a thread.",
            "Give the tester one realistic goal and ask them to think aloud. Avoid explaining where to click; record their words and pauses, then revise the most confusing step and test again.",
            "A vector stores elements contiguously and supports quick indexed access; a linked list connects nodes and makes certain insertions straightforward but requires traversal to reach an index. Try timing/searching small examples to compare.",
            "Pick one question first, such as attendance by month, and build a clean summary plus a trend chart. Add slicers only when they answer a real question, and check labels, units, and data types.",
            "Learn pwd, ls, cd, mkdir, cp, mv, rm, cat, grep, and man gradually. Use a temporary practice folder and check the current directory before deleting or moving anything.",
            "TypeScript is especially helpful when objects, API responses, or shared function inputs become hard to track. Start by typing one data shape and let the editor show where assumptions do not match.",
            "Use separate member, event, and registration tables, with registration connecting a member to an event. Give each row an ID and reference IDs rather than repeating full names in every registration.",
            "Start from text and background contrast, use one accent color, and test buttons and small labels at their actual size. A simple palette with consistent spacing usually looks more polished than many colors.",
            "Use a small sample project and trusted base images, then build and run one container at a time. Read commands before running them, and avoid mounting sensitive folders or exposing ports you do not need.",
            "Check a confusion matrix and per-class precision/recall, then compare against a simple baseline. Keep evaluation data separate from training data and explain which errors matter for your use case.",
            "Try the smallest reproducible example, change one thing at a time, and write down what you expected versus what happened. Sharing the exact error and relevant snippet helps others give focused feedback."
    );
}
