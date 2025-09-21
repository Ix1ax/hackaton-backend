package ru.ixlax.hackaton.core;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ixlax.hackaton.api.p2p.P2PPublisher;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.domain.entity.Incident;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentStatus;
import ru.ixlax.hackaton.domain.repository.IncidentRepo;
import ru.ixlax.hackaton.sse.SseHub;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IncidentsTtlJob {

    private final IncidentRepo incidents;
    private final SseHub sse;
    private final P2PPublisher p2p;

    // проверяем TTL/истечение каждую секунду, чтобы инциденты "пропадали" вовремя
    @Scheduled(fixedDelayString = "${app.incidents.ttl-check-ms:1000}")
    public void tick() {
        long now = System.currentTimeMillis();
        List<Incident> active = incidents.findByStatusInAndTtlSecNotNull(
                List.of(IncidentStatus.NEW, IncidentStatus.CONFIRMED)
        );
        for (Incident e : active) {
            Integer ttl = e.getTtlSec();
            if (ttl == null) continue;
            if (e.getStatus() == IncidentStatus.RESOLVED) continue;

            if (e.getTs() + ttl * 1000L <= now) {
                e.setStatus(IncidentStatus.RESOLVED);
                incidents.save(e);

                var dto = new IncidentDto(
                        e.getId(), e.getExternalId(), e.getObjectId(),
                        e.getLevel(), e.getKind(), e.getReason(),
                        e.getLat(), e.getLng(), e.getTs(), e.getStatus(),
                        e.getRegionCode(), e.getOriginRegion()
                );
                sse.publish(dto);
                p2p.broadcastIncidents(List.of(dto));
            }
        }
    }
}