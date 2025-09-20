package ru.ixlax.hackaton.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Profile("!stub")
@RequiredArgsConstructor
public class RealAIService implements AIService {
    private final WebClient aiWebClient;

    @Value("${ai.model:deepseek/deepseek-chat}")
    private String model;

    @Value("${ai.temperature:0.2}")
    private double temperature;

    @Override
    public Mono<String> chat(String user) {
        var messages = List.of(new ChatDtos.Message("user", user));
        var req = new ChatDtos.ChatRequest(model, messages, temperature, false, null, null);
        return call(req);
    }

    @Override
    public Mono<String> chatWithContext(String system, String user, String ctxJson) {
        var messages = List.of(
                new ChatDtos.Message("system", system + "\n\nКонтекст: " + ctxJson),
                new ChatDtos.Message("user", user)
        );
        var req = new ChatDtos.ChatRequest(model, messages, temperature, false, null, null);
        return call(req);
    }

    private Mono<String> call(ChatDtos.ChatRequest req) {
        return aiWebClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ChatDtos.ChatResponse.class)
                .map(r -> r.choices() == null || r.choices().isEmpty()
                        ? ""
                        : r.choices().get(0).message().content());
    }
}