package ru.ixlax.hackaton.api.p2p;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.p2p.dto.EventEnvelope;
import ru.ixlax.hackaton.api.publicapi.dto.*;
import ru.ixlax.hackaton.domain.entity.*;
import ru.ixlax.hackaton.domain.repository.*;
import ru.ixlax.hackaton.sse.SseHub;

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
    private final CameraRepo cameraRepo;

    private final SseHub sse;

    @PostMapping("/events")
    public Map<String,Object> accept(@RequestBody List<EventEnvelope> batch){
        int ok=0, bad=0;
        for (var env : batch){
            try {
                switch (String.valueOf(env.type())) {
                    case "INCIDENT" -> {
                        var dtos = mapper.convertValue(env.payload(), new TypeReference<List<IncidentDto>>(){});
                        for (var dto : dtos){
                            IncidentDto savedOrUpdated = upsertIncident(dto);
                            if (savedOrUpdated != null) {
                                // ⚡ публикуем в локальный SSE => WsBridge разнесёт в /topic/**
                                sse.publish(savedOrUpdated);
                            }
                            ok++;
                        }
                    }
                    case "NEWS" -> {
                        var dtos = mapper.convertValue(env.payload(), new TypeReference<List<NewsDto>>(){});
                        for (var dto : dtos){
                            NewsDto savedOrUpdated = upsertNews(dto);
                            if (savedOrUpdated != null) {
                                sse.publishNews(savedOrUpdated);
                            }
                            ok++;
                        }
                    }
                    case "PLACE" -> {
                        var dtos = mapper.convertValue(env.payload(), new TypeReference<List<PlaceDto>>(){});
                        for (var dto : dtos){ upsertPlace(dto); ok++; }
                    }
                    case "SENSOR" -> {
                        var dtos = mapper.convertValue(env.payload(), new TypeReference<List<SensorDto>>(){});
                        for (var dto : dtos){ upsertSensor(dto); ok++; }
                    }
                    case "CAMERA" -> {
                        var dtos = mapper.convertValue(env.payload(), new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ru.ixlax.hackaton.api.publicapi.dto.CameraDto>>(){});
                        for (var dto : dtos){ upsertCamera(dto); ok++; }
                    }
                    default -> bad++;
                }
            } catch (Exception e) {
                bad++;
            }
        }
        return Map.of("accepted", ok, "rejected", bad);
    }

    private IncidentDto upsertIncident(IncidentDto d){
        try {
            var e = incidentRepo.findByExternalId(d.externalId()).orElseGet(Incident::new);
            boolean isNew = (e.getExternalId() == null);
            if (isNew) e.setExternalId(d.externalId());

            if (!isNew && e.getTs() != 0 && e.getTs() > d.ts()) return null;

            e.setObjectId(d.objectId());
            e.setLevel(d.level());
            e.setKind(d.kind());
            e.setReason(d.reason());
            e.setLat(d.lat()); e.setLng(d.lng());
            e.setTs(d.ts()); e.setStatus(d.status());
            e.setRegionCode(d.regionCode()); e.setOriginRegion(d.originRegion());

            Incident saved = incidentRepo.save(e);

            return new IncidentDto(
                    saved.getId(), saved.getExternalId(), saved.getObjectId(),
                    saved.getLevel(), saved.getKind(), saved.getReason(),
                    saved.getLat(), saved.getLng(), saved.getTs(), saved.getStatus(),
                    saved.getRegionCode(), saved.getOriginRegion()
            );
        } catch (DataIntegrityViolationException dup) {
            // одновременно вставили — перечитываем и при необходимости обновляем
            return incidentRepo.findByExternalId(d.externalId()).map(existing -> {
                if (existing.getTs() < d.ts()) {
                    existing.setObjectId(d.objectId());
                    existing.setLevel(d.level());
                    existing.setKind(d.kind());
                    existing.setReason(d.reason());
                    existing.setLat(d.lat()); existing.setLng(d.lng());
                    existing.setTs(d.ts()); existing.setStatus(d.status());
                    existing.setRegionCode(d.regionCode()); existing.setOriginRegion(d.originRegion());
                    Incident saved = incidentRepo.save(existing);
                    return new IncidentDto(
                            saved.getId(), saved.getExternalId(), saved.getObjectId(),
                            saved.getLevel(), saved.getKind(), saved.getReason(),
                            saved.getLat(), saved.getLng(), saved.getTs(), saved.getStatus(),
                            saved.getRegionCode(), saved.getOriginRegion()
                    );
                }
                return null;
            }).orElse(null);
        }
    }

    private NewsDto upsertNews(NewsDto d){
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
        n.setStatus(d.status());
        News saved = newsRepo.save(n);

        return new NewsDto(
                saved.getId(), saved.getTs(), saved.getTitle(), saved.getBody(),
                saved.getRegionCode(), saved.getSource(), saved.getIncidentExternalId(),
                saved.getPlaceId(), saved.getLat(), saved.getLng(), saved.getStatus()
        );
    }

    private void upsertPlace(PlaceDto d){
        var p = placeRepo.findByExternalId(d.externalId()).orElseGet(Place::new);
        if (p.getExternalId()==null) p.setExternalId(d.externalId());
        if (p.getUpdatedAt()!=0 && p.getUpdatedAt()>d.updatedAt()) return;
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

    private void upsertCamera(ru.ixlax.hackaton.api.publicapi.dto.CameraDto d){
        var c = cameraRepo.findByExternalId(d.externalId()).orElseGet(ru.ixlax.hackaton.domain.entity.Camera::new);
        if (c.getExternalId()==null) c.setExternalId(d.externalId());
        if (c.getUpdatedAt()!=0 && c.getUpdatedAt()>d.updatedAt()) return;
        c.setName(d.name());
        c.setRegionCode(d.regionCode());
        c.setLat(d.lat()); c.setLng(d.lng());
        c.setRadiusM(d.radiusM());
        c.setUrl(d.url());
        c.setPublicUrl(d.publicUrl());
        c.setSnapshotUrl(d.snapshotUrl());
        c.setUpdatedAt(d.updatedAt());
        cameraRepo.save(c);
    }
}