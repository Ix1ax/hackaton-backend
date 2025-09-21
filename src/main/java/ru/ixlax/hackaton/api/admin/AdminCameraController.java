package ru.ixlax.hackaton.api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.p2p.P2PPublisher;
import ru.ixlax.hackaton.api.publicapi.dto.CameraAlertDto;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.core.NotificationService;
import ru.ixlax.hackaton.core.VisionAnalyzer;
import ru.ixlax.hackaton.domain.entity.Camera;
import ru.ixlax.hackaton.domain.entity.Incident;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentKind;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentLevel;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentStatus;
import ru.ixlax.hackaton.domain.repository.CameraRepo;
import ru.ixlax.hackaton.domain.repository.IncidentRepo;
import ru.ixlax.hackaton.sse.SseHub;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/camera")
public class AdminCameraController {

    private final VisionAnalyzer analyzer;
    private final CameraRepo cameras;
    private final IncidentRepo incidentRepo;
    private final P2PPublisher p2p;
    private final SseHub sse;
    private final NotificationService notification;

    @PostMapping("/detect")
    public String detect(@RequestParam(required = false) Long id,
                         @RequestParam(required = false) String externalId,
                         @RequestParam(required = false) String snapshotUrl,
                         @RequestParam(defaultValue = "true") boolean createIncident) throws Exception {

        Optional<Camera> camOpt = (id != null)
                ? cameras.findById(id)
                : (externalId != null ? cameras.findByExternalId(externalId) : Optional.empty());

        Camera c = camOpt.orElseThrow();

        var hit = analyzer.detect(c, null, snapshotUrl);
        if (hit.isEmpty()) return "OK";

        String stream = (c.getPublicUrl()!=null && !c.getPublicUrl().isBlank()) ? c.getPublicUrl() : c.getUrl();
        sse.publishCameraAlert(new CameraAlertDto(System.currentTimeMillis(), null, c.getId(), c.getName(), stream, c.getLat(), c.getLng()));

        if (createIncident) {
            long now = System.currentTimeMillis();
            var active = incidentRepo.findTopByObjectIdAndStatusInOrderByTsDesc(
                    "camera:"+c.getId(), List.of(IncidentStatus.NEW, IncidentStatus.CONFIRMED)
            ).orElse(null);
            if (active == null || now - active.getTs() > 5 * 60_000) {
                var e = new Incident();
                e.setExternalId(UUID.randomUUID().toString());
                e.setObjectId("camera:"+c.getId());
                e.setLevel(IncidentLevel.MEDIUM);
                e.setKind(IncidentKind.UNKNOWN);
                e.setReason(hit.get());
                e.setLat(c.getLat()); e.setLng(c.getLng());
                e.setTs(now);
                e.setRegionCode(c.getRegionCode());
                e.setOriginRegion(c.getRegionCode());
                var saved = incidentRepo.save(e);

                var dto = new IncidentDto(
                        saved.getId(), saved.getExternalId(), saved.getObjectId(),
                        saved.getLevel(), saved.getKind(), saved.getReason(),
                        saved.getLat(), saved.getLng(), saved.getTs(), saved.getStatus(),
                        saved.getRegionCode(), saved.getOriginRegion()
                );
                sse.publish(dto);
                p2p.broadcastIncidents(List.of(dto));
                notification.incidentRaised(saved);
            }
        }
        return "HIT: " + hit.get();
    }
}