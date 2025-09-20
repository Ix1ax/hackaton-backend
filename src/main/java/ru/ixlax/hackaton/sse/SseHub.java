package ru.ixlax.hackaton.sse;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.api.publicapi.dto.NewsDto;

@Component
public class SseHub {

    private final Sinks.Many<ServerSentEvent<IncidentDto>> incidentSink =
            Sinks.many().multicast().onBackpressureBuffer();

    private final Sinks.Many<ServerSentEvent<Object>> eventSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public Flux<ServerSentEvent<IncidentDto>> stream() {
        return incidentSink.asFlux();
    }

    public Flux<ServerSentEvent<Object>> events() {
        return eventSink.asFlux();
    }

    public void publish(IncidentDto dto) {
        var incidentEv = ServerSentEvent.builder(dto)
                .event("incident")
                .build();
        incidentSink.tryEmitNext(incidentEv);

        var allEv = ServerSentEvent.<Object>builder()
                .event("incident")
                .data(dto)
                .build();
        eventSink.tryEmitNext(allEv);
    }

    public void publishNews(NewsDto dto) {
        var ev = ServerSentEvent.<Object>builder()
                .event("news")
                .data(dto)
                .build();
        eventSink.tryEmitNext(ev);
    }

    public void publishSensor(Object dto) {
        var ev = ServerSentEvent.<Object>builder()
                .event("sensor")
                .data(dto)
                .build();
        eventSink.tryEmitNext(ev);
    }
}