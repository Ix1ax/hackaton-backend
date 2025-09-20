package ru.ixlax.hackaton.api.p2p;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.ixlax.hackaton.api.p2p.dto.EventEnvelope;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor
@Service
public class P2PPublisher {
    private final WebClient web = WebClient.builder().build();
    private final PeersService peersService;

    @Value("${app.node.id}") String nodeId;

    private final Queue<EventEnvelope> outbox = new ConcurrentLinkedQueue<>();

    public void broadcastIncidents(List<IncidentDto> dtos){
        if (dtos==null || dtos.isEmpty()) return;
        outbox.add(new EventEnvelope("INCIDENT", nodeId, System.currentTimeMillis(), dtos, null));
        drain();
    }

    @Scheduled(fixedDelay = 3000)
    public void drain(){
        var peers = peersService.getPeers();
        if (peers.isEmpty()) return;

        while (!outbox.isEmpty()){
            var env = outbox.peek();
            boolean anyOk = false;
            for (var peer : peers){
                try {
                    web.post().uri(peer + "/p2p/events")
                            .bodyValue(List.of(env))
                            .retrieve().toBodilessEntity()
                            .timeout(java.time.Duration.ofSeconds(2))
                            .block();
                    anyOk = true;
                } catch (Exception ignored) {}
            }
            if (anyOk) outbox.poll(); else break;
        }
    }

    public void broadcastNews(java.util.List<ru.ixlax.hackaton.api.publicapi.dto.NewsDto> dtos){
        if (dtos==null || dtos.isEmpty()) return;
        outbox.add(new EventEnvelope("NEWS", nodeId, System.currentTimeMillis(), dtos, null));
        drain();
    }
}