package ru.ixlax.hackaton.api.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.api.publicapi.dto.NewsDto;
import ru.ixlax.hackaton.domain.entity.*;
import ru.ixlax.hackaton.domain.repository.*;
import ru.ixlax.hackaton.sse.SseHub;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicController {
    private final IncidentRepo incidentRepo;
    private final PlaceRepo placeRepo;
    private final NewsRepo newsRepo;
    private final SseHub sse;

    @GetMapping("/incidents")
    public List<IncidentDto> incidents(@RequestParam double minLat, @RequestParam double minLng,
                                       @RequestParam double maxLat, @RequestParam double maxLng,
                                       @RequestParam(required = false) Long since) {
        long s = since == null ? 0 : since;
        return incidentRepo.findByBboxSince(s, minLat, maxLat, minLng, maxLng)
                .stream().map(this::toDto).toList();
    }

    @GetMapping("/places")
    public List<Place> places(@RequestParam String region) {
        if(region == null || region.isEmpty()) {
            return placeRepo.findAll();
        }
        return placeRepo.findByRegionCode(region);
    }

    @GetMapping("/news")
    public List<NewsDto> news(@RequestParam String region) {
        if(region == null || region.isEmpty()) {
            return newsRepo.findAll().stream().map(this::toDto).toList();
        }
        return newsRepo.findTop200ByRegionCodeOrderByTsDesc(region)
                .stream().map(this::toDto).toList();
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> events() { return sse.events(); }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<IncidentDto>> stream() { return sse.stream(); }

    private IncidentDto toDto(Incident e) {
        return new IncidentDto(e.getId(), e.getExternalId(), e.getObjectId(),
                e.getLevel(), e.getKind(), e.getReason(),
                e.getLat(), e.getLng(), e.getTs(), e.getStatus(),
                e.getRegionCode(), e.getOriginRegion());
    }
    private NewsDto toDto(News n){
        return new NewsDto(n.getId(), n.getTs(), n.getTitle(), n.getBody(),
                n.getRegionCode(), n.getSource(), n.getIncidentExternalId(),
                n.getPlaceId(), n.getLat(), n.getLng(), n.getStatus());
    }
}