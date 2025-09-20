package ru.ixlax.hackaton.api.p2p;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.p2p.dto.EventEnvelope;
import ru.ixlax.hackaton.api.publicapi.dto.*;
import ru.ixlax.hackaton.domain.entity.*;
import ru.ixlax.hackaton.domain.repository.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/p2p")
public class P2PController {
    private final IncidentRepo incidentRepo;
    private final PlaceRepo placeRepo;
    private final SensorRepo sensorRepo;
    private final NewsRepo newsRepo;
    private final ObjectMapper mapper;

    @PostMapping("/events")
    public Map<String,Object> accept(@RequestBody List<EventEnvelope> batch){
        int ok=0, bad=0;
        for (var env : batch){
            try {
                switch (String.valueOf(env.type())) {
                    case "INCIDENT" -> {
                        var dtos = mapper.convertValue(env.payload(), new TypeReference<List<IncidentDto>>(){});
                        for (var dto : dtos){ upsertIncident(dto); ok++; }
                    }
                    case "NEWS" -> {
                        var dtos = mapper.convertValue(env.payload(), new TypeReference<List<NewsDto>>(){});
                        for (var dto : dtos){ upsertNews(dto); ok++; }
                    }
                    case "PLACE" -> {
                        var dtos = mapper.convertValue(env.payload(), new TypeReference<List<PlaceDto>>(){});
                        for (var dto : dtos){ upsertPlace(dto); ok++; }
                    }
                    case "SENSOR" -> {
                        var dtos = mapper.convertValue(env.payload(), new TypeReference<List<SensorDto>>(){});
                        for (var dto : dtos){ upsertSensor(dto); ok++; }
                    }
                    default -> bad++;
                }
            } catch (Exception e) {
                bad++;
            }
        }
        return Map.of("accepted", ok, "rejected", bad);
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

    private void upsertPlace(PlaceDto d){
        var p = placeRepo.findByExternalId(d.externalId()).orElseGet(Place::new);
        if (p.getExternalId()==null) p.setExternalId(d.externalId());
        if (p.getUpdatedAt()!=0 && p.getUpdatedAt()>d.updatedAt()) return; // берём самый свежий
        p.setType(d.type());
        p.setName(d.name());
        p.setAddress(d.address());
        p.setLat(d.lat()); p.setLng(d.lng());
        p.setCapacity(d.capacity());
        p.setRegionCode(d.regionCode());
        p.setUpdatedAt(d.updatedAt());
        placeRepo.save(p);
    }

    private void upsertSensor(SensorDto d){
        var s = sensorRepo.findByExternalId(d.externalId()).orElseGet(Sensor::new);
        if (s.getExternalId()==null) s.setExternalId(d.externalId());
        if (s.getUpdatedAt()!=0 && s.getUpdatedAt()>d.updatedAt()) return;
        s.setName(d.name());
        s.setType(d.type());
        s.setLat(d.lat()); s.setLng(d.lng());
        s.setRegionCode(d.regionCode());
        s.setMeta(d.meta());
        s.setUpdatedAt(d.updatedAt());
        sensorRepo.save(s);
    }
}