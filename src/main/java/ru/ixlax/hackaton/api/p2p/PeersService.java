package ru.ixlax.hackaton.api.p2p;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeersService {

    private final WebClient web = WebClient.builder().build();

    @Value("${app.node.peers:}") private String peersCsv;
    @Value("${app.node.registry-url:}") private String registry;

    private final AtomicReference<List<String>> peers = new AtomicReference<>(List.of());

    public List<String> getPeers() {
        var p = peers.get();
        if (p.isEmpty()) return parseCsv(peersCsv);
        return p;
    }

    @Scheduled(fixedDelay = 5000)
    public void refresh() {
        if (registry != null && !registry.isBlank()) {
            try {
                var resp = web.get().uri(registry).retrieve().bodyToMono(Registry.class).block();
                if (resp != null && resp.peers != null && !resp.peers.isEmpty()) {
                    peers.set(clean(resp.peers));
                    return;
                }
            } catch (Exception ignored) {}
        }
        peers.set(parseCsv(peersCsv));
    }

    private static List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return clean(Arrays.stream(csv.split(",")).map(String::trim).toList());
    }
    private static List<String> clean(List<String> in) {
        return in.stream().filter(s -> !s.isBlank()).distinct().collect(Collectors.toList());
    }

    private record Registry(List<String> peers) {}
}