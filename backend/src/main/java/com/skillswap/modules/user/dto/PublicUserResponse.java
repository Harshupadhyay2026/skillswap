package com.skillswap.modules.user.dto;

import com.skillswap.modules.user.entity.Role;
import com.skillswap.modules.user.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Public-safe view of a user. Deliberately excludes email and any
 * credential-adjacent fields so it can be embedded in responses that
 * other users are allowed to read (e.g. swap proposals).
 */
public class PublicUserResponse {

    private Long id;
    private String fullName;
    private String username;
    private String college;
    private String branch;
    private String yearOfStudy;
    private String city;
    private String bio;
    private String avatarUrl;
    private String learningMode;
    private Set<String> roles;

    public PublicUserResponse() {}

    public static PublicUserResponse fromEntity(User user) {
        if (user == null) return null;
        PublicUserResponse res = new PublicUserResponse();
        res.setId(user.getId());
        res.setFullName(user.getFullName());
        res.setUsername(user.getUsername());
        res.setCollege(user.getCollege());
        res.setBranch(user.getBranch());
        res.setYearOfStudy(user.getYearOfStudy());
        res.setCity(user.getCity());
        res.setBio(user.getBio());
        res.setAvatarUrl(user.getAvatarUrl());
        res.setLearningMode(user.getLearningMode());
        res.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getLearningMode() { return learningMode; }
    public void setLearningMode(String learningMode) { this.learningMode = learningMode; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
}
