package ru.ixlax.hackaton.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.ixlax.hackaton.domain.entity.Camera;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.camera.monitor.analyzer", havingValue = "openrouter")
public class VisionAnalyzerOpenRouter implements VisionAnalyzer {

    @Value("${app.ai.openrouter.key:}")      String apiKey;
    @Value("${app.ai.openrouter.model:openai/gpt-4o-mini}") String defaultModel;

    @Value("${app.camera.monitor.snapshot-base:http://gateway/snapshots/}")
    String snapshotBase;

    @Value("${app.camera.monitor.snapshot-max-age-sec:600}")
    long maxAgeSec;

    @Override
    public Optional<String> detect(Camera c, String userPrompt, String explicitSnapshotUrl) {
        try {
            if (apiKey == null || apiKey.isBlank()) return Optional.empty();
            WebClient web = WebClient.builder().build();

            // 1) выбираем URL кадра
            String snap = explicitSnapshotUrl;
            if (snap == null || snap.isBlank()) {
                if (c.getSnapshotUrl() != null && !c.getSnapshotUrl().isBlank()) {
                    snap = c.getSnapshotUrl();
                } else {
                    String slug = (c.getExternalId()!=null && !c.getExternalId().isBlank())
                            ? c.getExternalId() : ("cam" + c.getId());
                    snap = snapshotBase + URLEncoder.encode(slug + ".jpg", StandardCharsets.UTF_8);
                }
            }

            // 2) проверяем что кадр существует и свежий
            ResponseEntity<Void> head = web.head().uri(snap).retrieve().toBodilessEntity().block();
            if (head == null || !head.getStatusCode().is2xxSuccessful()) return Optional.empty();

            String lm = head.getHeaders().getFirst("Last-Modified");
            if (lm != null) {
                Instant last = ZonedDateTime.parse(lm, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                if (Instant.now().minusSeconds(maxAgeSec).isAfter(last)) return Optional.empty();
            }

            // 3) базовый промпт
            String basePrompt = """
Посмотри на снимок. Если видишь дым, пламя, затопление, ДТП, массовое скопление людей, оружие или неестественные цвета кожи — верни КОРОТКУЮ фразу-при原因 на русском (без пояснений).
Если всё нормально — верни ровно "OK".
""".trim();
            String prompt = (userPrompt == null || userPrompt.isBlank()) ? basePrompt : (basePrompt + "\n" + userPrompt);

            // 4) модель
            String model = defaultModel;

            // 5) запрос
            String body = """
{
  "model": "%s",
  "messages": [{
    "role": "user",
    "content": [
      {"type":"text","text": %s},
      {"type":"image_url","image_url":{"url": %s}}
    ]
  }],
  "temperature": 0.1
}
""".formatted(model, json(prompt), json(snap));

            String resp = web.post()
                    .uri("https://openrouter.ai/api/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (resp == null) return Optional.empty();

            // очень простой парс: достаём content
            String lower = resp.toLowerCase();
            // если модель где-то вернула "ok" — игнор
            if (lower.contains("\"ok\"") || lower.matches("(?s).*\\bok\\b.*")) return Optional.empty();

            // иначе вернём сырой текст ответа (фронту/джобе пригодится для regex)
            return Optional.of(resp);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String json(String s){
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n") + "\"";
    }
}