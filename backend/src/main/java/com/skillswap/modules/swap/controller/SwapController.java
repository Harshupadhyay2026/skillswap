package com.skillswap.modules.swap.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.swap.dto.CreateSwapRequest;
import com.skillswap.modules.swap.dto.SwapResponse;
import com.skillswap.modules.swap.service.SwapService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/swaps")
public class SwapController {

    private final SwapService swapService;

    public SwapController(SwapService swapService) {
        this.swapService = swapService;
    }

    /** POST /api/swaps - Propose a new skill swap */
    @PostMapping
    public ResponseEntity<ApiResponse<SwapResponse>> proposeSwap(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateSwapRequest req) {
        SwapResponse response = swapService.proposeSwap(principal.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Skill swap proposal sent successfully", response));
    }

    /** GET /api/swaps/incoming - Get proposals received by logged-in user */
    @GetMapping("/incoming")
    public ResponseEntity<ApiResponse<List<SwapResponse>>> getIncomingSwaps(
            @AuthenticationPrincipal UserDetails principal) {
        List<SwapResponse> list = swapService.getIncomingSwaps(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Incoming swaps fetched", list));
    }

    /** GET /api/swaps/outgoing - Get proposals sent by logged-in user */
    @GetMapping("/outgoing")
    public ResponseEntity<ApiResponse<List<SwapResponse>>> getOutgoingSwaps(
            @AuthenticationPrincipal UserDetails principal) {
        List<SwapResponse> list = swapService.getOutgoingSwaps(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Outgoing swaps fetched", list));
    }

    /** GET /api/swaps/all - Get all swaps (both sent and received) */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SwapResponse>>> getAllMySwaps(
            @AuthenticationPrincipal UserDetails principal) {
        List<SwapResponse> list = swapService.getAllMySwaps(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("All user swaps fetched", list));
    }

    /** GET /api/swaps/{id} - one swap, participants only (used by the video room page) */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SwapResponse>> getSwap(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        SwapResponse response = swapService.getSwap(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Swap fetched", response));
    }

    /** PUT /api/swaps/{id}/accept - Accept a swap proposal */
    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<SwapResponse>> acceptSwap(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        SwapResponse response = swapService.acceptSwap(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Swap request accepted! Meeting room created.", response));
    }

    /** PUT /api/swaps/{id}/decline - Decline a swap proposal */
    @PutMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<SwapResponse>> declineSwap(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        SwapResponse response = swapService.declineSwap(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Swap request declined.", response));
    }

    /** PUT /api/swaps/{id}/complete - Mark swap session completed */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<SwapResponse>> completeSwap(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        SwapResponse response = swapService.completeSwap(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Swap session marked as completed!", response));
    }

    /** PUT /api/swaps/{id}/cancel - Cancel pending proposal */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SwapResponse>> cancelSwap(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        SwapResponse response = swapService.cancelSwap(principal.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Swap request cancelled.", response));
    }
}
