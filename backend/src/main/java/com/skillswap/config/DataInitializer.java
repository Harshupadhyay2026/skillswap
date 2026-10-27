package com.skillswap.config;

import com.skillswap.modules.skill.entity.Skill;
import com.skillswap.modules.skill.repository.SkillRepository;
import com.skillswap.modules.user.entity.Role;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.RoleRepository;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public DataInitializer(RoleRepository roleRepository,
                           SkillRepository skillRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.skillRepository = skillRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdmin();
        seedSkills();
    }

    private void seedRoles() {
        List<String> defaultRoles = List.of("ROLE_USER", "ROLE_TUTOR", "ROLE_ADMIN");
        for (String roleName : defaultRoles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
            }
        }
    }

    private void seedAdmin() {
        if (!StringUtils.hasText(adminUsername)) return;

        String username = adminUsername.trim().toLowerCase();
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

        User existing = userRepository.findByUsername(username).orElse(null);
        if (existing == null && StringUtils.hasText(adminEmail)) {
            // Reuse an existing account if the email is already registered.
            existing = userRepository.findByEmail(adminEmail.trim().toLowerCase()).orElse(null);
        }
        if (existing != null) {
            existing.getRoles().add(adminRole);
            userRepository.save(existing);
            return;
        }

        // Create the account only when credentials are explicitly provided.
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) return;

        User admin = new User();
        admin.setFullName("SkillSwap Moderator");
        admin.setUsername(username);
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setCollege("SkillSwap");
        admin.setLearningMode("Both");

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        admin.getRoles().add(userRole);
        admin.getRoles().add(adminRole);
        userRepository.save(admin);
    }

    private void seedSkills() {
        List<Skill> skills = List.of(
            // Programming
            new Skill("Python",      "Programming",    "🐍"),
            new Skill("Java",        "Programming",    "☕"),
            new Skill("C",           "Programming",    "🔧"),
            new Skill("C++",         "Programming",    "⚡"),
            new Skill("JavaScript",  "Programming",    "🌐"),
            new Skill("TypeScript",  "Programming",    "🔷"),
            new Skill("Rust",        "Programming",    "🦀"),
            new Skill("Go",          "Programming",    "🐹"),

            // Web Development
            new Skill("React",       "Web Dev",        "⚛️"),
            new Skill("Vue.js",      "Web Dev",        "💚"),
            new Skill("Angular",     "Web Dev",        "🔴"),
            new Skill("Node.js",     "Web Dev",        "🟢"),
            new Skill("Spring Boot", "Web Dev",        "🍃"),
            new Skill("HTML & CSS",  "Web Dev",        "🎨"),
            new Skill("Next.js",     "Web Dev",        "▲"),

            // Data & AI
            new Skill("Machine Learning",  "Data & AI", "🤖"),
            new Skill("Deep Learning",     "Data & AI", "🧠"),
            new Skill("Data Analysis",     "Data & AI", "📊"),
            new Skill("SQL",               "Data & AI", "🗄️"),
            new Skill("MongoDB",           "Data & AI", "🍃"),
            new Skill("Power BI",          "Data & AI", "📈"),

            // CS Fundamentals
            new Skill("DSA",          "CS Fundamentals", "🔢"),
            new Skill("OS Concepts",  "CS Fundamentals", "💻"),
            new Skill("Networking",   "CS Fundamentals", "🌐"),
            new Skill("DBMS",         "CS Fundamentals", "🗃️"),

            // Design & Tools
            new Skill("Figma",        "Design",        "🎭"),
            new Skill("Adobe XD",     "Design",        "💜"),
            new Skill("UI/UX Design", "Design",        "✏️"),
            new Skill("Canva",        "Design",        "🖌️"),

            // DevOps & Cloud
            new Skill("Git & GitHub", "DevOps",        "🐙"),
            new Skill("Docker",       "DevOps",        "🐳"),
            new Skill("AWS",          "DevOps",        "☁️"),
            new Skill("Linux",        "DevOps",        "🐧")
        );

        for (Skill skill : skills) {
            if (skillRepository.findByNameIgnoreCase(skill.getName()).isEmpty()) {
                skillRepository.save(skill);
            }
        }
    }
}
