package ru.ixlax.hackaton.ai;

public interface AIService {
    reactor.core.publisher.Mono<String> chat(String userPrompt);
    reactor.core.publisher.Mono<String> chatWithContext(String systemPrompt, String userPrompt, String contextJson);
}