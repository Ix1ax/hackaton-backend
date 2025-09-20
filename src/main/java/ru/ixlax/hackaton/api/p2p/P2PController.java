package ru.ixlax.hackaton.api.p2p;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.p2p.dto.EventEnvelope;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.api.publicapi.dto.NewsDto;
import ru.ixlax.hackaton.domain.entity.Incident;
import ru.ixlax.hackaton.domain.entity.News;
import ru.ixlax.hackaton.domain.repository.IncidentRepo;
import ru.ixlax.hackaton.domain.repository.NewsRepo;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/p2p")
public class P2PController {
    private final IncidentRepo incidentRepo;
    private final NewsRepo newsRepo;
    private final ObjectMapper mapper;

    @PostMapping("/events")
    public Map<String,Object> accept(@RequestBody List<EventEnvelope> batch){
        int ok=0, bad=0;
        for (var env : batch) {
            switch (String.valueOf(env.type())) {
                case "INCIDENT" -> {
                    var dtos = mapper.convertValue(env.payload(), new TypeReference<List<IncidentDto>>(){});
                    for (var d : dtos) { upsertIncident(d); ok++; }
                }
                case "NEWS" -> {
                    var dtos = mapper.convertValue(env.payload(), new TypeReference<List<NewsDto>>(){});
                    for (var d : dtos) { upsertNews(d); ok++; }
                }
                default -> bad++;
            }
        }
        return Map.of("accepted", ok, "rejected", bad);
    }

    @GetMapping("/sync/incidents")
    public List<IncidentDto> sync(@RequestParam long since){
        return incidentRepo.findByTsGreaterThanOrderByTsAsc(since)
                .stream().map(this::toDto).toList();
    }

    private void upsertIncident(IncidentDto d){
        var e = incidentRepo.findByExternalId(d.externalId()).orElseGet(Incident::new);
        if (e.getExternalId()==null) e.setExternalId(d.externalId());
        if (e.getTs()!=0 && e.getTs()>d.ts()) return;
        e.setObjectId(d.objectId());
        e.setLevel(d.level());
        e.setKind(d.kind());
        e.setReason(d.reason());
        e.setLat(d.lat()); e.setLng(d.lng());
        e.setTs(d.ts()); e.setStatus(d.status());
        e.setRegionCode(d.regionCode()); e.setOriginRegion(d.originRegion());
        incidentRepo.save(e);
    }
    private void upsertNews(NewsDto d){
        var n = newsRepo.findFirstByTsAndSourceAndTitle(d.ts(), d.source(), d.title())
                .orElseGet(News::new);
        n.setTs(d.ts());
        n.setTitle(d.title());
        n.setBody(d.body());
        n.setRegionCode(d.regionCode());
        n.setSource(d.source());
        n.setIncidentExternalId(d.incidentExternalId());
        n.setPlaceId(d.placeId());
        n.setLat(d.lat()); n.setLng(d.lng());
        newsRepo.save(n);
    }

    private IncidentDto toDto(Incident e){
        return new IncidentDto(e.getId(), e.getExternalId(), e.getObjectId(),
                e.getLevel(), e.getKind(), e.getReason(),
                e.getLat(), e.getLng(), e.getTs(), e.getStatus(),
                e.getRegionCode(), e.getOriginRegion());
    }
}