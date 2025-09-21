package ru.ixlax.hackaton.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.ixlax.hackaton.api.publicapi.dto.*;
import ru.ixlax.hackaton.domain.entity.*;
import ru.ixlax.hackaton.domain.repository.*;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SyncOnStartup implements CommandLineRunner {
    private final IncidentRepo incidents;
    private final NewsRepo newsRepo;
    private final PlaceRepo placeRepo;
    private final SensorRepo sensorRepo;
    private final CameraRepo cameraRepo;

    private final ObjectMapper mapper;
    private final WebClient web = WebClient.builder().build();

    @Value("${app.node.peers:}") String peersCsv;

    @Override public void run(String... args) {
        var peers = Arrays.stream(peersCsv.split(","))
                .map(String::trim).filter(s->!s.isBlank()).toList();
        if (peers.isEmpty()) return;

        syncIncidents(peers);
        syncNews(peers);
        syncPlaces(peers);
        syncSensors(peers);
        syncCameras(peers);
    }

    private void syncIncidents(List<String> peers) {
        long since = incidents.findTop200ByOrderByTsDesc().stream()
                .mapToLong(Incident::getTs).max().orElse(0L);
        for (var p : peers) {
            try {
                var body = web.get().uri(p + "/p2p/sync/incidents?since=" + since)
                        .retrieve().bodyToMono(String.class).block();
                var dtos = mapper.readValue(body, new TypeReference<List<IncidentDto>>() {});
                for (var d : dtos) upsertIncident(d);
            } catch (Exception ignored) {}
        }
    }

    private void syncNews(List<String> peers) {
        long since = newsRepo.findTop200ByRegionCodeOrderByTsDesc("%").stream()
                .mapToLong(News::getTs).max().orElse(0L);
        for (var p : peers) {
            try {
                var body = web.get().uri(p + "/p2p/sync/news?since=" + since)
                        .retrieve().bodyToMono(String.class).block();
                var dtos = mapper.readValue(body, new TypeReference<List<NewsDto>>() {});
                for (var d : dtos) upsertNews(d);
            } catch (Exception ignored) {}
        }
    }

    private void syncPlaces(List<String> peers) {
        long since = placeRepo.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(0).stream()
                .mapToLong(Place::getUpdatedAt).max().orElse(0L);
        for (var p : peers) {
            try {
                var body = web.get().uri(p + "/p2p/sync/places?since=" + since)
                        .retrieve().bodyToMono(String.class).block();
                var dtos = mapper.readValue(body, new TypeReference<List<PlaceDto>>() {});
                for (var d : dtos) upsertPlace(d);
            } catch (Exception ignored) {}
        }
    }

    private void syncSensors(List<String> peers) {
        long since = sensorRepo.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(0).stream()
                .mapToLong(Sensor::getUpdatedAt).max().orElse(0L);
        for (var p : peers) {
            try {
                var body = web.get().uri(p + "/p2p/sync/sensors?since=" + since)
                        .retrieve().bodyToMono(String.class).block();
                var dtos = mapper.readValue(body, new TypeReference<List<SensorDto>>() {});
                for (var d : dtos) upsertSensor(d);
            } catch (Exception ignored) {}
        }
    }

    private void syncCameras(List<String> peers) {
        long since = cameraRepo.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(0).stream()
                .mapToLong(Camera::getUpdatedAt).max().orElse(0L);
        for (var p : peers) {
            try {
                var body = web.get().uri(p + "/p2p/sync/cameras?since=" + since)
                        .retrieve().bodyToMono(String.class).block();
                var dtos = mapper.readValue(body, new TypeReference<List<CameraDto>>() {});
                for (var d : dtos) upsertCamera(d);
            } catch (Exception ignored) {}
        }
    }

    private void upsertIncident(IncidentDto d){
        try {
            var e = incidents.findByExternalId(d.externalId()).orElseGet(Incident::new);
            if (e.getExternalId()==null) e.setExternalId(d.externalId());
            if (e.getTs()!=0 && e.getTs()>d.ts()) return;
            e.setObjectId(d.objectId()); e.setLevel(d.level()); e.setKind(d.kind()); e.setReason(d.reason());
            e.setLat(d.lat()); e.setLng(d.lng()); e.setTs(d.ts()); e.setStatus(d.status());
            e.setRegionCode(d.regionCode()); e.setOriginRegion(d.originRegion());
            incidents.save(e);
        } catch (DataIntegrityViolationException dup) {
            incidents.findByExternalId(d.externalId()).ifPresent(existing -> {
                if (existing.getTs() < d.ts()) {
                    existing.setObjectId(d.objectId());
                    existing.setLevel(d.level());
                    existing.setKind(d.kind());
                    existing.setReason(d.reason());
                    existing.setLat(d.lat()); existing.setLng(d.lng());
                    existing.setTs(d.ts()); existing.setStatus(d.status());
                    existing.setRegionCode(d.regionCode()); existing.setOriginRegion(d.originRegion());
                    incidents.save(existing);
                }
            });
        }
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
        n.setStatus(d.status());
        newsRepo.save(n);
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

    private void upsertCamera(CameraDto d){
        var c = cameraRepo.findByExternalId(d.externalId()).orElseGet(Camera::new);
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