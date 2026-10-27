package com.skillswap.modules.auth.service;

import com.skillswap.exception.ConflictException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.auth.dto.AuthResponse;
import com.skillswap.modules.auth.dto.LoginRequest;
import com.skillswap.modules.auth.dto.RegisterRequest;
import com.skillswap.modules.user.dto.UserResponse;
import com.skillswap.modules.user.entity.Role;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.RoleRepository;
import com.skillswap.modules.user.repository.UserRepository;
import com.skillswap.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().toLowerCase().trim();
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered: " + email);
        }

        User user = new User();
        user.setFullName(request.getName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCollege(request.getCollege().trim());
        user.setBranch(request.getBranch() != null ? request.getBranch().trim() : "Student");
        user.setYearOfStudy(request.getYear() != null ? request.getYear().trim() : "1st Year");
        user.setCity(request.getCity() != null ? request.getCity().trim() : "India");
        user.setBio(request.getBio() != null ? request.getBio().trim() : "");
        user.setLearningMode(request.getMode() != null ? request.getMode().trim() : "Both");

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        roles.add(userRole);

        if ("Teach".equalsIgnoreCase(request.getMode()) || "Both".equalsIgnoreCase(request.getMode())) {
            Role tutorRole = roleRepository.findByName("ROLE_TUTOR")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_TUTOR")));
            roles.add(tutorRole);
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        String token = tokenProvider.generateTokenFromUsername(savedUser.getUsername());

        return new AuthResponse(token, UserResponse.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail().trim(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail().trim(), request.getUsernameOrEmail().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new AuthResponse(token, UserResponse.fromEntity(user));
    }
}
