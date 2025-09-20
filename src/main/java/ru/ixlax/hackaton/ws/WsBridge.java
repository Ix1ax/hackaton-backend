package ru.ixlax.hackaton.ws;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.Disposable;
import ru.ixlax.hackaton.sse.SseHub;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WsBridge {
    private final SseHub sse;
    private final SimpMessagingTemplate tpl;
    private Disposable subIncidents;
    private Disposable subEvents;

    @PostConstruct
    public void init() {
        // только инциденты
        subIncidents = sse.stream().subscribe(ev -> {
            tpl.convertAndSend("/topic/incidents", ev.data());
            tpl.convertAndSend("/topic/all", Map.of("type", "incident", "data", ev.data()));
        });
        // все события: incident/news/sensor
        subEvents = sse.events().subscribe((ServerSentEvent<?> ev) -> {
            String type = ev.event();
            Object data = ev.data();
            if ("news".equals(type))    tpl.convertAndSend("/topic/news", data);
            if ("sensor".equals(type))  tpl.convertAndSend("/topic/sensors", data);
            if ("incident".equals(type))tpl.convertAndSend("/topic/incidents", data);
            tpl.convertAndSend("/topic/all", Map.of("type", type, "data", data));
        });
    }
}