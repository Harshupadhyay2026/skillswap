package com.skillswap.modules.problem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateAnswerRequest {

    @NotBlank(message = "Answer body is required")
    @Size(max = 10000, message = "Answer is too long")
    private String body;

    public CreateAnswerRequest() {}

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
