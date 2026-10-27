package com.skillswap.modules.session.controller;

import com.skillswap.common.ApiResponse;
import com.skillswap.modules.session.dto.SessionNoteRequest;
import com.skillswap.modules.session.dto.SessionNoteResponse;
import com.skillswap.modules.session.service.SessionNoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Shared notes for swap and learning sessions.
 *
 * The path uses the same {@code swap|learning} value that the video room page
 * uses, so the frontend can reuse its route parameter directly.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionNoteController {

    private final SessionNoteService noteService;

    public SessionNoteController(SessionNoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/{sessionType}/{sessionId}/notes")
    public ResponseEntity<ApiResponse<List<SessionNoteResponse>>> list(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String sessionType,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Session notes fetched",
                noteService.list(principal.getUsername(), sessionType, sessionId)));
    }

    @PostMapping("/{sessionType}/{sessionId}/notes")
    public ResponseEntity<ApiResponse<SessionNoteResponse>> create(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String sessionType,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionNoteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Session note added",
                noteService.create(principal.getUsername(), sessionType, sessionId, req)));
    }

    @PutMapping("/{sessionType}/{sessionId}/notes/{noteId}")
    public ResponseEntity<ApiResponse<SessionNoteResponse>> update(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String sessionType,
            @PathVariable Long sessionId,
            @PathVariable Long noteId,
            @Valid @RequestBody SessionNoteRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Session note updated",
                noteService.update(principal.getUsername(), sessionType, sessionId, noteId, req)));
    }

    @DeleteMapping("/{sessionType}/{sessionId}/notes/{noteId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String sessionType,
            @PathVariable Long sessionId,
            @PathVariable Long noteId) {
        noteService.delete(principal.getUsername(), sessionType, sessionId, noteId);
        return ResponseEntity.ok(ApiResponse.ok("Session note deleted", null));
    }
}
