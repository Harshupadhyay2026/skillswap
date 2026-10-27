package com.skillswap.modules.community.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.community.dto.TopContributorResponse;
import com.skillswap.modules.community.service.CommunityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public community endpoints — no account needed to see who helps out. */
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    /** GET /api/community/top-contributors?limit=20 */
    @GetMapping("/top-contributors")
    public ResponseEntity<ApiResponse<List<TopContributorResponse>>> topContributors(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Top contributors fetched",
                communityService.topContributors(limit)));
    }
}
