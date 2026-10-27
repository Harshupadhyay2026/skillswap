package com.skillswap.modules.user.dto;

import com.skillswap.modules.user.entity.Role;
import com.skillswap.modules.user.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

public class UserResponse {

    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String college;
    private String branch;
    private String yearOfStudy;
    private String city;
    private String bio;
    private String learningMode;
    private String avatarUrl;
    private Set<String> roles;

    public UserResponse() {}

    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setCollege(user.getCollege());
        response.setBranch(user.getBranch());
        response.setYearOfStudy(user.getYearOfStudy());
        response.setCity(user.getCity());
        response.setBio(user.getBio());
        response.setLearningMode(user.getLearningMode());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRoles(user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet()));
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(String yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLearningMode() {
        return learningMode;
    }

    public void setLearningMode(String learningMode) {
        this.learningMode = learningMode;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
