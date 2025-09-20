package ru.ixlax.hackaton.api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.api.p2p.P2PPublisher;
import ru.ixlax.hackaton.domain.entity.*;
import ru.ixlax.hackaton.domain.entity.enums.incident.*;
import ru.ixlax.hackaton.domain.repository.*;
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

    @PostMapping("/spawn")
    public IncidentDto spawn(@RequestParam double lat, @RequestParam double lng,
                             @RequestParam(defaultValue = "CRITICAL") IncidentLevel level,
                             @RequestParam(defaultValue = "UNKNOWN")  IncidentKind  kind,
                             @RequestParam(defaultValue = "manual")   String reason,
                             @RequestParam(defaultValue = "RU-MOW")    String region) {
        var e = new Incident();
        e.setExternalId(UUID.randomUUID().toString());
        e.setLevel(level); e.setKind(kind); e.setReason(reason);
        e.setLat(lat); e.setLng(lng); e.setTs(System.currentTimeMillis());
        e.setRegionCode(region); e.setOriginRegion(region);
        e = incidentRepo.save(e);

        var dto = toDto(e);
        // авто-новость
        var n = new News();
        n.setTs(e.getTs());
        n.setTitle("Обнаружена аномалия: " + e.getKind());
        n.setBody("Уровень: " + e.getLevel() + ". Причина: " + e.getReason());
        n.setRegionCode(e.getRegionCode());
        n.setSource("DISPATCHER");
        n.setIncidentExternalId(e.getExternalId());
        n.setLat(e.getLat()); n.setLng(e.getLng());
        newsRepo.save(n);

        sse.publish(dto);
        p2p.broadcastIncidents(List.of(dto));
        return dto;
    }

    public record StatusDto(IncidentStatus status) {}
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