package com.skillswap.modules.user.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.user.dto.UserResponse;
import com.skillswap.modules.user.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse user = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully", user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.skillswap.modules.user.dto.UpdateProfileRequest request) {
        UserResponse updated = userService.updateCurrentUser(request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }

    /** POST /api/users/me/avatar — multipart form field named "file". */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        UserResponse updated = userService.uploadAvatar(file);
        return ResponseEntity.ok(ApiResponse.ok("Profile photo updated", updated));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> removeAvatar() {
        UserResponse updated = userService.removeAvatar();
        return ResponseEntity.ok(ApiResponse.ok("Profile photo removed", updated));
    }
}
