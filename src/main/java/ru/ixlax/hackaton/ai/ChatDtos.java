package ru.ixlax.hackaton.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

public class ChatDtos {

    public record Message(String role, String content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatRequest(
            @JsonProperty("model") String model,
            List<Message> messages,
            Double temperature,
            Boolean stream,
            @JsonProperty("max_tokens") Integer maxTokens,
            List<String> stop
    ) {}

    public record Choice(Message message) {}
    public record ChatResponse(String id, String model, List<Choice> choices) {}
}