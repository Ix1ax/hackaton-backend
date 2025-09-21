package ru.ixlax.hackaton.api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/incidents")
public class AdminIncidentsController {
    private final IncidentRepo incidentRepo;
    private final NewsRepo newsRepo;
    private final SseHub sse;
    private final P2PPublisher p2p;
    private final CameraService cameraService;

    @Value("${app.incidents.default-ttl-sec:40}")
    private int defaultTtlSec;

    @PostMapping("/spawn")
    public IncidentDto spawn(@RequestParam double lat, @RequestParam double lng,
                             @RequestParam(defaultValue = "HIGH")    IncidentLevel level,
                             @RequestParam(defaultValue = "UNKNOWN") IncidentKind  kind,
                             @RequestParam(defaultValue = "manual")  String reason,
                             @RequestParam(defaultValue = "RU-MOW")  String region,
                             @RequestParam(required = false) Integer ttlSec) {

        int ttl = (ttlSec == null) ? defaultTtlSec : Math.max(1, ttlSec);

        var e = new Incident();
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
        var dto = toDto(saved);

        var n = new News();
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
        final String reg = saved.getRegionCode();
        cameraService.pickFor(latVal, lngVal, reg)
                .ifPresent(cam -> cameraService.publishAlert(extId, cam, latVal, lngVal));

        return dto;
    }

    public record StatusDto(ru.ixlax.hackaton.domain.entity.enums.incident.IncidentStatus status) {}
    @PatchMapping("/{id}/status")
    public void setStatus(@PathVariable Long id, @RequestBody StatusDto body) {
        incidentRepo.findById(id).ifPresent(e -> { e.setStatus(body.status()); incidentRepo.save(e); });
    }

    private IncidentDto toDto(Incident e) {
        return new IncidentDto(e.getId(), e.getExternalId(), e.getObjectId(),
                e.getLevel(), e.getKind(), e.getReason(),
                e.getLat(), e.getLng(), e.getTs(), e.getStatus(),
                e.getRegionCode(), e.getOriginRegion());
    }
}