package ru.ixlax.hackaton.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import ru.ixlax.hackaton.domain.repository.IncidentRepo;
import ru.ixlax.hackaton.domain.repository.NewsRepo;
import ru.ixlax.hackaton.sse.SseHub;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PublicIncidentsWsController {

    private final IncidentRepo incidentRepo;
    private final NewsRepo newsRepo;
    private final SseHub sse;
    private final P2PPublisher p2p;
    private final CameraService cameraService;

    @Value("${app.incidents.default-ttl-sec:40}")
    private int defaultTtlSec;

    public record ReportPayload(
            Double lat,
            Double lng,
            IncidentLevel level,
            IncidentKind kind,
            String reason,
            String region,
            Integer ttlSec
    ) {}

    @MessageMapping("/incidents/report")
    public void report(ReportPayload in) {
        final double lat = in.lat() == null ? 0 : in.lat();
        final double lng = in.lng() == null ? 0 : in.lng();
        final IncidentLevel level = in.level() == null ? IncidentLevel.LOW : in.level();
        final IncidentKind kind = in.kind() == null ? IncidentKind.UNKNOWN : in.kind();
        final String reason = (in.reason() == null || in.reason().isBlank()) ? "user-report" : in.reason();
        final String region = (in.region() == null || in.region().isBlank()) ? "RU-MOW" : in.region();
        final int ttl = in.ttlSec() == null ? defaultTtlSec : Math.max(1, in.ttlSec());

        Incident e = new Incident();
        e.setExternalId(UUID.randomUUID().toString());
        e.setLevel(level);
        e.setKind(kind);
        e.setReason(reason);
        e.setLat(lat);
        e.setLng(lng);
        e.setTs(System.currentTimeMillis());
        e.setRegionCode(region);
        e.setOriginRegion(region);
        e.setTtlSec(ttl);

        final Incident saved = incidentRepo.save(e);
        final IncidentDto dto = toDto(saved);

        News n = new News();
        n.setTs(saved.getTs());
        n.setTitle("Поступило сообщение об аномалии: " + saved.getKind());
        n.setBody("Уровень: " + saved.getLevel() + ". Источник: dispetcher. Причина: " + saved.getReason());
        n.setRegionCode(saved.getRegionCode());
        n.setSource("USER");
        n.setIncidentExternalId(saved.getExternalId());
        n.setLat(saved.getLat());
        n.setLng(saved.getLng());
        n = newsRepo.save(n);

        sse.publish(dto);
        p2p.broadcastIncidents(List.of(dto));

        NewsDto newsDto = new NewsDto(
                n.getId(), n.getTs(), n.getTitle(), n.getBody(),
                n.getRegionCode(), n.getSource(), n.getIncidentExternalId(),
                n.getPlaceId(), n.getLat(), n.getLng(), n.getStatus()
        );
        sse.publishNews(newsDto);
        p2p.broadcastNews(List.of(newsDto));

        final String extId = saved.getExternalId();
        final double latVal = saved.getLat();
        final double lngVal = saved.getLng();
        final String reg = saved.getRegionCode();
        cameraService.pickFor(latVal, lngVal, reg)
                .ifPresent(cam -> cameraService.publishAlert(extId, cam, latVal, lngVal));
    }

    private static IncidentDto toDto(Incident e) {
        return new IncidentDto(e.getId(), e.getExternalId(), e.getObjectId(),
                e.getLevel(), e.getKind(), e.getReason(),
                e.getLat(), e.getLng(), e.getTs(), e.getStatus(),
                e.getRegionCode(), e.getOriginRegion());
    }
}