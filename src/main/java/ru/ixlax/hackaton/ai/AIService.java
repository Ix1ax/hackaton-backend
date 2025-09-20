package ru.ixlax.hackaton.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static ru.ixlax.hackaton.ai.ChatDtos.*;

@Service
@RequiredArgsConstructor
public class AIService {

    private final WebClient aiWebClient;

    @Value("${ai.model:deepseek/deepseek-chat}")
    private String model;

    @Value("${ai.temperature:0.2}")
    private double temperature;

    public Mono<String> chat(String system, String user) {
        var messages = List.of(
                new Message("system", system),
                new Message("user", user)
        );
        var req = new ChatRequest(model, messages, temperature, false, null, null);

        return aiWebClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(resp -> resp.choices() == null || resp.choices().isEmpty()
                        ? ""
                        : resp.choices().get(0).message().content());
    }
}