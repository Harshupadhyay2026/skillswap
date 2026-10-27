package com.skillswap.modules.chat.dto;

import jakarta.validation.constraints.NotNull;

public class StartConversationRequest {

    @NotNull(message = "Choose someone to message")
    private Long userId;

    public StartConversationRequest() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
