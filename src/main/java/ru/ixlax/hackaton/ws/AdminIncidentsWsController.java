package ru.ixlax.hackaton.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import ru.ixlax.hackaton.api.p2p.P2PPublisher;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.api.publicapi.dto.NewsDto;
import ru.ixlax.hackaton.core.CameraService;
import ru.ixlax.hackaton.domain.entity.Incident;
import ru.ixlax.hackaton.domain.entity.News;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentKind;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentLevel;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentStatus;
import ru.ixlax.hackaton.domain.repository.IncidentRepo;
import ru.ixlax.hackaton.domain.repository.NewsRepo;
import ru.ixlax.hackaton.sse.SseHub;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AdminIncidentsWsController {

    private final IncidentRepo incidentRepo;
    private final NewsRepo newsRepo;
    private final SseHub sse;
    private final P2PPublisher p2p;
    private final CameraService cameraService;

    @Value("${app.incidents.default-ttl-sec:40}")
    private int defaultTtlSec;

    public record SpawnPayload(double lat, double lng, IncidentLevel level, IncidentKind kind, String reason, String region, Integer ttlSec) {}

    @MessageMapping("/admin/incidents/spawn")
    public void spawn(SpawnPayload in) {
        int ttl = (in.ttlSec() == null) ? defaultTtlSec : Math.max(1, in.ttlSec());

        Incident e = new Incident();
        e.setExternalId(UUID.randomUUID().toString());
        e.setLevel(in.level() == null ? IncidentLevel.LOW : in.level());
        e.setKind(in.kind() == null ? IncidentKind.UNKNOWN : in.kind());
        e.setReason(in.reason() == null ? "manual" : in.reason());
        e.setLat(in.lat());
        e.setLng(in.lng());
        e.setTs(System.currentTimeMillis());
        String reg = in.region() == null ? "RU-MOW" : in.region();
        e.setRegionCode(reg);
        e.setOriginRegion(reg);
        e.setTtlSec(ttl);

        final Incident saved = incidentRepo.save(e);
        IncidentDto dto = toDto(saved);

        News n = new News();
        n.setTs(saved.getTs());
        n.setTitle("Обнаружена аномалия: " + saved.getKind());
        n.setBody("Уровень: " + saved.getLevel() + ". Причина: " + saved.getReason());
        n.setRegionCode(saved.getRegionCode());
        n.setSource("DISPATCHER");
        n.setIncidentExternalId(saved.getExternalId());
        n.setLat(saved.getLat());
        n.setLng(saved.getLng());
        n = newsRepo.save(n);

        sse.publish(dto);
        p2p.broadcastIncidents(List.of(dto));

        var newsDto = new NewsDto(
                n.getId(), n.getTs(), n.getTitle(), n.getBody(),
                n.getRegionCode(), n.getSource(), n.getIncidentExternalId(),
                n.getPlaceId(), n.getLat(), n.getLng(), n.getStatus()
        );
        sse.publishNews(newsDto);
        p2p.broadcastNews(List.of(newsDto));

        final String extId = saved.getExternalId();
        final double latVal = saved.getLat();
        final double lngVal = saved.getLng();
        final String regVal = saved.getRegionCode();
        cameraService.pickFor(latVal, lngVal, regVal)
                .ifPresent(cam -> cameraService.publishAlert(extId, cam, latVal, lngVal));
    }

    public record StatusPayload(IncidentStatus status) {}

    @MessageMapping("/admin/incidents/{id}/status")
    public void setStatus(@DestinationVariable Long id, StatusPayload body) {
        incidentRepo.findById(id).ifPresent(e -> {
            e.setStatus(body.status());
            incidentRepo.save(e);
            sse.publish(toDto(e));
        });
    }

    private static IncidentDto toDto(Incident e) {
        return new IncidentDto(e.getId(), e.getExternalId(), e.getObjectId(),
                e.getLevel(), e.getKind(), e.getReason(),
                e.getLat(), e.getLng(), e.getTs(), e.getStatus(),
                e.getRegionCode(), e.getOriginRegion());
    }
}