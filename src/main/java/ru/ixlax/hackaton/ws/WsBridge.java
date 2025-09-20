package ru.ixlax.hackaton.ws;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.api.publicapi.dto.NewsDto;
import ru.ixlax.hackaton.api.publicapi.dto.SensorStatusDto;
import ru.ixlax.hackaton.core.SensorCache;
import ru.ixlax.hackaton.sse.SseHub;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WsBridge {

    private final SseHub sse;
    private final SimpMessagingTemplate tpl;
    private final SensorCache sensorCache;

    private Disposable sub;

    public record SensorLite(
            Long id, String name, String type, double lat, double lng,
            String health, String message, long ts
    ) {}

    public record Frame(
            long ts,
            List<IncidentDto> incidents,
            List<NewsDto> news,
            List<SensorLite> sensors,
            Map<String,Integer> meta
    ) {}

    @PostConstruct
    public void init() {

        Flux<List<IncidentDto>> incidentsBatch =
                sse.events()
                        .filter(ev -> "incident".equals(ev.event()))
                        .map(ServerSentEvent::data)
                        .cast(IncidentDto.class)
                        .onBackpressureBuffer(2000)
                        .bufferTimeout(200, Duration.ofSeconds(2))
                        .map(list -> list.stream().limit(200).toList())
                        .startWith(List.of());

        Flux<List<NewsDto>> newsBatch =
                sse.events()
                        .filter(ev -> "news".equals(ev.event()))
                        .map(ServerSentEvent::data)
                        .cast(NewsDto.class)
                        .onBackpressureBuffer(2000)
                        .bufferTimeout(100, Duration.ofSeconds(2))
                        .map(list -> list.stream().limit(100).toList())
                        .startWith(List.of());

        Flux<List<SensorLite>> sensorsBatch =
                sse.events()
                        .filter(ev -> "sensor".equals(ev.event()))
                        .map(ServerSentEvent::data)
                        .cast(SensorStatusDto.class)
                        .groupBy(SensorStatusDto::sensorId)
                        .flatMap(g -> g.sample(Duration.ofSeconds(10)))
                        .map(this::toSensorLite)
                        .bufferTimeout(500, Duration.ofSeconds(10))
                        .map(buf -> {
                            Map<Long, SensorLite> last = new LinkedHashMap<>();
                            for (SensorLite s : buf) last.put(s.id(), s); // последние по id
                            return last.values().stream().limit(200).collect(Collectors.toList());
                        })
                        .startWith(List.of());

        Flux<Frame> frames =
                Flux.combineLatest(
                        Arrays.asList(incidentsBatch, newsBatch, sensorsBatch),
                        arr -> {
                            @SuppressWarnings("unchecked") List<IncidentDto> inc = (List<IncidentDto>) arr[0];
                            @SuppressWarnings("unchecked") List<NewsDto>     nw  = (List<NewsDto>)     arr[1];
                            @SuppressWarnings("unchecked") List<SensorLite>  sen = (List<SensorLite>)  arr[2];
                            return new Frame(
                                    System.currentTimeMillis(),
                                    inc, nw, sen,
                                    Map.of("incidents", inc.size(),
                                            "news",     nw.size(),
                                            "sensors",  sen.size())
                            );
                        }
                ).sample(Duration.ofSeconds(2));

        sub = frames.subscribe(f -> tpl.convertAndSend("/topic/all", f));
    }

    private SensorLite toSensorLite(SensorStatusDto st) {
        var basic = sensorCache.get(st.sensorId());
        String name = basic != null ? basic.name() : ("Sensor #" + st.sensorId());
        String type = st.type();
        double lat  = basic != null ? basic.lat() : 0.0;
        double lng  = basic != null ? basic.lng() : 0.0;
        return new SensorLite(
                st.sensorId(), name, type, lat, lng,
                String.valueOf(st.health()), st.message(), st.ts()
        );
    }
}