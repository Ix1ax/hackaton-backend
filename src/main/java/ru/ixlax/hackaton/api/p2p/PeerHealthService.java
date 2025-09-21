package ru.ixlax.hackaton.api.p2p;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PeerHealthService {

    @Value("${NODE_ID:unknown}")
    private String nodeId;

    @Value("${REGION_CODE:RU-UNK}")
    private String regionCode;

    @Value("${PEERS:}")
    private String peersRaw;
    private final RestClient http = RestClient.create();

    public record Status(
            String nodeId,
            String regionCode,
            String ip,
            List<String> peers,
            String status
    ) {}

    public Status selfPing() {
        List<String> peers = parsePeers(peersRaw);
        String ip = findMyIpOrNull();

        return new Status(
                (nodeId == null || nodeId.isBlank()) ? "unknown" : nodeId,
                (regionCode == null || regionCode.isBlank()) ? "RU-UNK" : regionCode,
                ip,
                Collections.unmodifiableList(peers),
                "OK"
        );
    }

    private static List<String> parsePeers(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String[] parts = raw.split(",");
        List<String> res = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) res.add(s);
        }
        return res;
    }

    private static String findMyIpOrNull() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignore) {
            return null;
        }
    }

    public List<Status> snapshot() {
        List<Status> out = new ArrayList<>();
        out.add(selfPing());

        for (String base : parsePeers(peersRaw)) {
            String url = (base.endsWith("/") ? base : base + "/") + "api/public/ping";
            try {
                Status st = http.get().uri(url).retrieve().body(Status.class);
                if (st != null) out.add(st);
            } catch (Exception e) {
                out.add(new Status(
                        base,
                        "UNKNOWN",
                        null,
                        List.of(),
                        "DOWN"
                ));
            }
        }
        return out;
    }
}