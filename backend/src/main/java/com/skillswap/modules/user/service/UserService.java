package com.skillswap.modules.user.service;

import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.modules.user.dto.UserResponse;
import com.skillswap.modules.user.entity.User;
import com.skillswap.modules.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AvatarStorageService avatarStorageService;

    public UserService(UserRepository userRepository,
                       AvatarStorageService avatarStorageService) {
        this.userRepository = userRepository;
        this.avatarStorageService = avatarStorageService;
    }

    public UserResponse getCurrentUser() {
        return UserResponse.fromEntity(currentUser());
    }

    public UserResponse updateCurrentUser(com.skillswap.modules.user.dto.UpdateProfileRequest req) {
        User user = currentUser();

        if (req.getFullName() != null && !req.getFullName().trim().isEmpty()) {
            user.setFullName(req.getFullName().trim());
        }
        if (req.getCollege() != null && !req.getCollege().trim().isEmpty()) {
            user.setCollege(req.getCollege().trim());
        }
        if (req.getBranch() != null) {
            user.setBranch(req.getBranch().trim());
        }
        if (req.getYearOfStudy() != null) {
            user.setYearOfStudy(req.getYearOfStudy().trim());
        }
        if (req.getCity() != null) {
            user.setCity(req.getCity().trim());
        }
        if (req.getBio() != null) {
            user.setBio(req.getBio().trim());
        }
        if (req.getLearningMode() != null) {
            user.setLearningMode(req.getLearningMode().trim());
        }
        // Avatar changes go through the upload endpoint, never a raw URL string.
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl().trim());
        }

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    /** Uploads a new avatar and removes the previous file, if we stored it. */
    @Transactional
    public UserResponse uploadAvatar(MultipartFile file) {
        User user = currentUser();

        String previous = user.getAvatarUrl();
        String newUrl = avatarStorageService.store(file, user.getId());

        user.setAvatarUrl(newUrl);
        User saved = userRepository.save(user);

        avatarStorageService.deleteIfOwned(previous, user.getId());
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse removeAvatar() {
        User user = currentUser();
        String previous = user.getAvatarUrl();

        // Clear the URL after a successful save; the file itself is best-effort.
        user.setAvatarUrl(null);
        User saved = userRepository.save(user);

        avatarStorageService.deleteIfOwned(previous, user.getId());
        return UserResponse.fromEntity(saved);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }

        String username = authentication.getName();
        return userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username));
    }
}
