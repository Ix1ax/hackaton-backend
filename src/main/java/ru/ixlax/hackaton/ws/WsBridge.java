package ru.ixlax.hackaton.ws;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import ru.ixlax.hackaton.api.publicapi.dto.CameraAlertDto;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.api.publicapi.dto.NewsDto;
import ru.ixlax.hackaton.api.publicapi.dto.SensorStatusDto;
import ru.ixlax.hackaton.core.SensorCache;
import ru.ixlax.hackaton.sse.SseHub;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WsBridge {

    private static final Logger log = LoggerFactory.getLogger(WsBridge.class);

    private final SseHub sse;
    private final SimpMessagingTemplate tpl;
    private final SensorCache sensorCache;

    private Disposable sub;

    @PostConstruct
    public void init() {

        Flux<IncidentDto> incidents = sse.events()
                .filter(ev -> "incident".equals(ev.event()))
                .map(ServerSentEvent::data)
                .cast(IncidentDto.class)
                .onErrorContinue((e, o) -> log.warn("incident stream error: {}", e.toString()))
                .share();

        incidents.subscribe(dto -> {
            tpl.convertAndSend("/topic/incidents", dto);
            tpl.convertAndSend("/topic/all", new WsEnvelope("INCIDENT", System.currentTimeMillis(), dto));
        });

        Flux<NewsDto> news = sse.events()
                .filter(ev -> "news".equals(ev.event()))
                .map(ServerSentEvent::data)
                .cast(NewsDto.class)
                .onErrorContinue((e, o) -> log.warn("news stream error: {}", e.toString()))
                .share();

        news.subscribe(dto -> {
            tpl.convertAndSend("/topic/news", dto);
            tpl.convertAndSend("/topic/all", new WsEnvelope("NEWS", System.currentTimeMillis(), dto));
        });

        Flux<SensorStatusDto> sensors = sse.events()
                .filter(ev -> "sensor".equals(ev.event()))
                .map(ServerSentEvent::data)
                .cast(SensorStatusDto.class)
                .onErrorContinue((e, o) -> log.warn("sensor stream error: {}", e.toString()))
                .share();

        sensors.subscribe(st -> {
            tpl.convertAndSend("/topic/sensors", st);
            tpl.convertAndSend("/topic/all", new WsEnvelope("SENSOR", System.currentTimeMillis(), st));
        });

        Flux<CameraAlertDto> cams = sse.events()
                .filter(ev -> "camera-alert".equals(ev.event()))
                .map(ServerSentEvent::data)
                .cast(CameraAlertDto.class)
                .onErrorContinue((e, o) -> log.warn("camera stream error: {}", e.toString()))
                .share();

        cams.subscribe(alert -> {
            tpl.convertAndSend("/topic/camera-alerts", alert);
            tpl.convertAndSend("/topic/all", new WsEnvelope("CAMERA_ALERT", System.currentTimeMillis(), alert));
        });

        sub = Flux.interval(Duration.ofSeconds(15))
                .subscribe(t -> tpl.convertAndSend("/topic/all",
                        new WsEnvelope("PING", System.currentTimeMillis(), null)));
    }
}