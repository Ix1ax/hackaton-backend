package ru.ixlax.hackaton.core;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ixlax.hackaton.ai.AIService;
import ru.ixlax.hackaton.api.p2p.P2PPublisher;
import ru.ixlax.hackaton.api.publicapi.dto.NewsDto;
import ru.ixlax.hackaton.domain.entity.News;
import ru.ixlax.hackaton.domain.entity.Sensor;
import ru.ixlax.hackaton.domain.repository.MeasurementRepo;
import ru.ixlax.hackaton.domain.repository.NewsRepo;
import ru.ixlax.hackaton.domain.repository.SensorRepo;
import ru.ixlax.hackaton.sse.SseHub;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiDigestJob {
    private final SensorRepo sensors;
    private final MeasurementRepo measurements;
    private final NewsRepo news;
    private final SseHub sse;
    private final AIService ai;
    private final P2PPublisher p2p;

    // раз в минуту
    @Scheduled(fixedDelay = 60_000)
    public void run(){
        long now = System.currentTimeMillis();
        long since = now - 10*60_000;

        List<Sensor> all = sensors.findAll();
        Map<String, List<Sensor>> byRegion = all.stream()
                .collect(Collectors.groupingBy(Sensor::getRegionCode));

        byRegion.forEach((region, list) -> {
            long total = list.size();
            long totalMsmt = list.isEmpty() ? 0 :
                    measurements.countBySensorIdInAndTsGreaterThan(
                            list.stream().map(Sensor::getId).toList(), since);

            // соберём короткий контекст в JSON-строку
            StringBuilder ctx = new StringBuilder();
            ctx.append("{\"region\":\"").append(region).append("\",")
                    .append("\"windowMin\":10,")
                    .append("\"sensorsTotal\":").append(total).append(",")
                    .append("\"measurements10m\":").append(totalMsmt).append(",")
                    .append("\"sensors\":[");
            for (int i=0;i<Math.min(10, list.size());i++){
                var s = list.get(i);
                if (i>0) ctx.append(',');
                ctx.append("{\"id\":").append(s.getId())
                        .append(",\"name\":\"").append(s.getName()).append('"')
                        .append(",\"type\":\"").append(s.getType()).append('"')
                        .append(",\"lat\":").append(s.getLat())
                        .append(",\"lng\":").append(s.getLng()).append('}');
            }
            ctx.append("]}");

            String system = "Ты пишешь краткие оперативные сводки для диспетчеров ЧС. Отвечай СУХО и по делу на русском.";
            String user = """
Сгенерируй абзац (1–3 предложения) сводки за последние 10 минут по региону из контекста. 
Подчеркни, есть ли тревожные сигналы/аномалии, если данных мало — так и скажи. Без HTML, только текст.
            """;

            String bodyAi = ai.chat(system, "Контекст: "+ctx + "\n\n"+ user)
                    .blockOptional().orElse("");

            String title = "AI-сводка датчиков ("+region+"): измерений за 10м = " + totalMsmt;
            if (bodyAi==null || bodyAi.isBlank()) {
                bodyAi = "Автосводка за 10 минут на " + Instant.ofEpochMilli(now) + ". Данных немного.";
            }

            var n = new News();
            n.setTs(now);
            n.setTitle(title);
            n.setBody(bodyAi);
            n.setRegionCode(region);
            n.setSource("AI");
            news.save(n);

            var dto = new NewsDto(n.getId(), n.getTs(), n.getTitle(), n.getBody(),
                    n.getRegionCode(), n.getSource(), n.getIncidentExternalId(),
                    n.getPlaceId(), n.getLat(), n.getLng(), n.getStatus());

            sse.publishNews(dto);
            p2p.broadcastNews(List.of(dto));
        });
    }
}