package com.skillswap.modules.skill.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.skill.dto.AddUserSkillRequest;
import com.skillswap.modules.skill.dto.SkillResponse;
import com.skillswap.modules.skill.dto.UserSkillResponse;
import com.skillswap.modules.skill.service.SkillService;
import com.skillswap.modules.user.dto.PublicProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    // ── Public Skill Catalog ─────────────────────────────────────────

    /** GET /api/skills?query=python&category=Programming */
    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> searchSkills(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(
                ApiResponse.ok("Skills fetched", skillService.searchSkills(query, category)));
    }

    /** GET /api/skills/categories */
    @GetMapping("/skills/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(
                ApiResponse.ok("Categories fetched", skillService.getCategories()));
    }

    // ── Marketplace – teachers ───────────────────────────────────────

    /** GET /api/marketplace?query=react&skillId=5 */
    @GetMapping("/marketplace")
    public ResponseEntity<ApiResponse<List<UserSkillResponse>>> searchTeachers(
            @RequestParam(required = false) Long skillId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(
                ApiResponse.ok("Marketplace fetched", skillService.searchTeachers(skillId, query)));
    }

    /** GET /api/marketplace/matches?offered=Python&wanted=React — true mutual swaps */
    @GetMapping("/marketplace/matches")
    public ResponseEntity<ApiResponse<List<UserSkillResponse>>> mutualMatches(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam String offered,
            @RequestParam String wanted) {
        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Log in to find mutual swap matches");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "Mutual matches fetched",
                skillService.findMutualSwapMatches(principal.getUsername(), offered, wanted)));
    }

    /** GET /api/users/{id}/profile — public profile with skills (no email) */
    @GetMapping("/users/{id}/profile")
    public ResponseEntity<ApiResponse<PublicProfileResponse>> publicProfile(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Profile fetched", skillService.getPublicProfile(id)));
    }

    // ── Authenticated: My Skills ─────────────────────────────────────

    /** GET /api/users/skills */
    @GetMapping("/users/skills")
    public ResponseEntity<ApiResponse<List<UserSkillResponse>>> getMySkills(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                ApiResponse.ok("Skills fetched", skillService.getMySkills(principal.getUsername())));
    }

    /** POST /api/users/skills */
    @PostMapping("/users/skills")
    public ResponseEntity<ApiResponse<UserSkillResponse>> addSkill(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody AddUserSkillRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Skill added",
                skillService.addSkillToUser(principal.getUsername(), req)));
    }

    /** DELETE /api/users/skills/{id} */
    @DeleteMapping("/users/skills/{id}")
    public ResponseEntity<ApiResponse<Void>> removeSkill(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        skillService.removeSkillFromUser(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Skill removed", null));
    }
}
