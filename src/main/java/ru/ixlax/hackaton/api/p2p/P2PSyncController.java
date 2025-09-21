package ru.ixlax.hackaton.api.p2p;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.publicapi.dto.*;
import ru.ixlax.hackaton.domain.repository.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/p2p/sync")
public class P2PSyncController {
    private final IncidentRepo incidentRepo;
    private final NewsRepo newsRepo;
    private final PlaceRepo placeRepo;
    private final SensorRepo sensorRepo;
    private final CameraRepo cameraRepo;

    @GetMapping("/incidents")
    public List<IncidentDto> incidents(@RequestParam long since) {
        return incidentRepo.findByTsGreaterThanOrderByTsAsc(since).stream()
                .map(e -> new IncidentDto(e.getId(), e.getExternalId(), e.getObjectId(),
                        e.getLevel(), e.getKind(), e.getReason(),
                        e.getLat(), e.getLng(), e.getTs(), e.getStatus(),
                        e.getRegionCode(), e.getOriginRegion())).toList();
    }

    @GetMapping("/news")
    public List<NewsDto> news(@RequestParam long since) {
        return newsRepo.findByTsGreaterThanOrderByTsAsc(since).stream()
                .map(n -> new NewsDto(n.getId(), n.getTs(), n.getTitle(), n.getBody(),
                        n.getRegionCode(), n.getSource(), n.getIncidentExternalId(),
                        n.getPlaceId(), n.getLat(), n.getLng(), n.getStatus())).toList();
    }

    @GetMapping("/places")
    public List<PlaceDto> places(@RequestParam long since) {
        return placeRepo.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(since).stream()
                .map(p -> new PlaceDto(p.getId(), p.getExternalId(), p.getType(), p.getName(),
                        p.getAddress(), p.getLat(), p.getLng(), p.getCapacity(),
                        p.getRegionCode(), p.getUpdatedAt())).toList();
    }

    @GetMapping("/sensors")
    public List<SensorDto> sensors(@RequestParam long since) {
        return sensorRepo.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(since).stream()
                .map(s -> new SensorDto(s.getId(), s.getExternalId(), s.getName(), s.getType(),
                        s.getLat(), s.getLng(), s.getRegionCode(), s.getMeta(), s.getUpdatedAt())).toList();
    }

    @GetMapping("/cameras")
    public List<CameraDto> cameras(@RequestParam long since) {
        return cameraRepo.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(since).stream()
                .map(CameraDto::fromEntity).toList();
    }
}