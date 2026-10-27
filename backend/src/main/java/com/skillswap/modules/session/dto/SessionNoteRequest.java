package com.skillswap.modules.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update payload for a shared session note. */
public class SessionNoteRequest {

    @Size(max = 120, message = "Title must be 120 characters or fewer")
    private String title;

    @NotBlank(message = "Write a note before saving")
    @Size(max = 2000, message = "Notes must be 2000 characters or fewer")
    private String content;

    @Size(max = 500, message = "Next step must be 500 characters or fewer")
    private String nextStep;

    public SessionNoteRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getNextStep() { return nextStep; }
    public void setNextStep(String nextStep) { this.nextStep = nextStep; }
}
