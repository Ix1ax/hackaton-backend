package ru.ixlax.hackaton.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.domain.entity.Incident;
import ru.ixlax.hackaton.domain.repository.IncidentRepo;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class SyncOnStartup implements CommandLineRunner {
    private final IncidentRepo incidents;
    private final ObjectMapper mapper;
    private final WebClient web = WebClient.builder().build();

    @Value("${app.node.peers:}") String peersCsv;

    @Override public void run(String... args) throws Exception {
        long since = incidents.findTop200ByOrderByTsDesc().stream()
                .mapToLong(Incident::getTs).max().orElse(0L);

        var peers = Arrays.stream(peersCsv.split(","))
                .map(String::trim).filter(s->!s.isBlank()).toList();

        for (var p : peers) {
            try {
                var body = web.get().uri(p + "/p2p/sync/incidents?since=" + since)
                        .retrieve().bodyToMono(String.class).block();
                var dtos = mapper.readValue(body, new TypeReference<java.util.List<IncidentDto>>() {});
                for (var d : dtos) upsert(d);
            } catch (Exception ignored) {}
        }
    }

    private void upsert(IncidentDto d){
        var e = incidents.findByExternalId(d.externalId()).orElseGet(Incident::new);
        if (e.getExternalId()==null) e.setExternalId(d.externalId());
        if (e.getTs()!=0 && e.getTs()>d.ts()) return;
        e.setObjectId(d.objectId()); e.setLevel(d.level()); e.setKind(d.kind()); e.setReason(d.reason());
        e.setLat(d.lat()); e.setLng(d.lng()); e.setTs(d.ts()); e.setStatus(d.status());
        e.setRegionCode(d.regionCode()); e.setOriginRegion(d.originRegion());
        incidents.save(e);
    }
}