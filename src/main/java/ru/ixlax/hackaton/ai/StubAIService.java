package ru.ixlax.hackaton.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Profile("stub")
public class StubAIService implements AIService {
    @Override
    public Mono<String> chat(String userPrompt) {
        return Mono.just("Заглушка: данных недостаточно. Мониторинг стабильный.");
    }

    @Override
    public Mono<String> chatWithContext(String systemPrompt, String userPrompt, String contextJson) {
        return Mono.just("Сводка (stub): критичных аномалий не обнаружено. Идёт наблюдение.");
    }
}