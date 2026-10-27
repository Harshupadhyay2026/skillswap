package com.skillswap.modules.system;

import com.skillswap.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> details = new HashMap<>();
        details.put("status", "UP");
        details.put("service", "SkillSwap Backend API");
        details.put("version", "1.0.0");
        details.put("serverTime", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.ok("SkillSwap API service is healthy and operational", details));
    }
}
