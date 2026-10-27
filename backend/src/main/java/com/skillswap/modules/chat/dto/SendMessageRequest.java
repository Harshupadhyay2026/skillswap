package com.skillswap.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String body;

    public SendMessageRequest() {}

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
