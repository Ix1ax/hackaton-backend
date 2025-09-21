package ru.ixlax.hackaton.api.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.api.publicapi.dto.NewsDto;
import ru.ixlax.hackaton.api.publicapi.dto.PageResponse;
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

    private static Pageable pageable(Integer page, Integer size) {
        int p = page == null ? 0 : Math.max(page, 0);
        int sz = size == null ? 25 : Math.min(Math.max(size, 1), 25);
        return PageRequest.of(p, sz);
    }

    @GetMapping("/incidents")
    public PageResponse<IncidentDto> incidents(@RequestParam double minLat, @RequestParam double minLng,
                                               @RequestParam double maxLat, @RequestParam double maxLng,
                                               @RequestParam(required = false) Long since,
                                               @RequestParam(required = false, defaultValue = "0") Integer page,
                                               @RequestParam(required = false, defaultValue = "25") Integer size) {

        long s = since == null ? 0 : since;
        Pageable pg = pageable(page, size);

        Page<Incident> res = incidentRepo.findActiveByBboxSince(
                s, minLat, maxLat, minLng, maxLng, pg
        );

        List<IncidentDto> items = res.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(items, res.getNumber(), res.getSize(), res.getTotalElements(), res.getTotalPages());
    }

    @GetMapping("/places")
    public PageResponse<Place> places(@RequestParam(required = false) String region,
                                      @RequestParam(required = false, defaultValue = "0") Integer page,
                                      @RequestParam(required = false, defaultValue = "25") Integer size) {

        Pageable pg = pageable(page, size);
        Page<Place> res = (region == null || region.isEmpty())
                ? placeRepo.findAll(pg)
                : placeRepo.findByRegionCode(region, pg);

        return new PageResponse<>(res.getContent(), res.getNumber(), res.getSize(), res.getTotalElements(), res.getTotalPages());
    }

    @GetMapping("/news")
    public PageResponse<NewsDto> news(@RequestParam(required = false) String region,
                                      @RequestParam(required = false, defaultValue = "0") Integer page,
                                      @RequestParam(required = false, defaultValue = "25") Integer size) {

        Pageable pg = pageable(page, size);
        Page<News> res = (region == null || region.isEmpty())
                ? newsRepo.findAll(pg)
                : newsRepo.findByRegionCodeOrderByTsDesc(region, pg);

        List<NewsDto> items = res.getContent().stream().map(this::toDto).toList();
        return new PageResponse<>(items, res.getNumber(), res.getSize(), res.getTotalElements(), res.getTotalPages());
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

    @GetMapping("/ping")
    public ru.ixlax.hackaton.api.p2p.PeerHealthService.Status ping(
            ru.ixlax.hackaton.api.p2p.PeerHealthService svc) {
        return svc.selfPing();
    }

    @GetMapping("/peers/health")
    public java.util.List<ru.ixlax.hackaton.api.p2p.PeerHealthService.Status> peersHealth(
            ru.ixlax.hackaton.api.p2p.PeerHealthService svc) {
        return svc.snapshot();
    }
}