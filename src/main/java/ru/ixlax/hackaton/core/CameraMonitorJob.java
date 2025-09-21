package ru.ixlax.hackaton.core;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ixlax.hackaton.api.publicapi.dto.CameraAlertDto;
import ru.ixlax.hackaton.api.publicapi.dto.IncidentDto;
import ru.ixlax.hackaton.api.p2p.P2PPublisher;
import ru.ixlax.hackaton.api.publicapi.dto.NewsDto;
import ru.ixlax.hackaton.domain.entity.*;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentStatus;
import ru.ixlax.hackaton.domain.repository.*;
import ru.ixlax.hackaton.sse.SseHub;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.camera.monitor.enabled", havingValue = "true", matchIfMissing = false)
public class CameraMonitorJob {

    private final CameraRepo cameras;
    private final CameraPolicyRepo policies;
    private final VisionAnalyzer vision;
    private final IncidentRepo incidents;
    private final SseHub sse;
    private final P2PPublisher p2p;
    private final NotificationService notificationService;
    private final CameraService cameraService;
    private final SimpMessagingTemplate ws;
    private final NewsRepo newsRepo;

    @Value("${app.camera.monitor.snapshot-base:http://gateway/snapshots/}")
    String snapshotBase;

    @Value("${app.camera.monitor.snapshot-max-age-sec:600}")
    long maxAgeSec;

    private final Map<Long, Boolean> lastOnline = new HashMap<>();
    private final Map<Long, Long> lastScanTs = new HashMap<>();

    @Scheduled(fixedDelayString = "${app.camera.monitor.period-ms:60000}")
    public void tick() {
        for (Camera c : cameras.findAll()) {
            try {
                var stat = probeSnapshot(c);
                Boolean prev = lastOnline.put(c.getId(), stat.online);
                if (prev == null || prev != stat.online) {
                    ws.convertAndSend("/topic/camera-status",
                            new ru.ixlax.hackaton.api.publicapi.dto.CameraStatusDto(
                                    c.getId(), c.getExternalId(), stat.online, stat.lastSeenTs, stat.snapshotUrl, stat.reason
                            ));
                }

                var policy = policies.findByCameraId(c.getId()).orElse(null);
                if (policy == null || !policy.isEnabled()) continue;

                if (!stat.online) continue;
                long now = System.currentTimeMillis();
                long last = lastScanTs.getOrDefault(c.getId(), 0L);
                int periodMs = Math.max(5, (policy.getIntervalSec()==null?60:policy.getIntervalSec())) * 1000;
                if (now - last < periodMs) continue;

                String promptSuffix = (policy.getPromptSuffix()==null) ? "" : policy.getPromptSuffix();
                var respOpt = vision.detect(c, promptSuffix, stat.snapshotUrl);
                lastScanTs.put(c.getId(), now);
                if (respOpt.isEmpty()) continue;

                String resp = respOpt.get();
                String textLower = resp.toLowerCase();

                String okRegex = (policy.getOkRegex()==null || policy.getOkRegex().isBlank())
                        ? "(?i)\\bok\\b" : policy.getOkRegex();
                if (textLower.matches("(?s).*" + okRegex + ".*")) continue;

                if (policy.getHitRegex()!=null && !policy.getHitRegex().isBlank()) {
                    if (!textLower.matches("(?s).*" + policy.getHitRegex().toLowerCase() + ".*")) {
                        sendCameraAlertOnly(c, stat, "vision: " + trimResp(resp));
                        continue;
                    }
                }

                if (policy.getMode() == ru.ixlax.hackaton.domain.entity.enums.sensor.SensorMode.MANUAL) {
                    sendCameraAlertOnly(c, stat, "vision: " + trimResp(resp));
                } else {
                    raiseIncidentFromCamera(c, policy, "vision: " + trimResp(resp));
                }

            } catch (Exception ignore) {}
        }
    }

    public Map<String,Object> scanOnce(Long cameraId){
        cameras.findById(cameraId).ifPresent(c -> lastScanTs.put(cameraId, 0L));
        tick();
        return Map.of("status","queued");
    }

    private record Probe(boolean online, Long lastSeenTs, String reason, String snapshotUrl){}

    private Probe probeSnapshot(Camera c){
        try {
            String snap = (c.getSnapshotUrl()!=null && !c.getSnapshotUrl().isBlank())
                    ? c.getSnapshotUrl()
                    : (snapshotBase + URLEncoder.encode(((c.getExternalId()!=null && !c.getExternalId().isBlank())
                    ? c.getExternalId() : ("cam"+c.getId())) + ".jpg", StandardCharsets.UTF_8));

            var head = org.springframework.web.reactive.function.client.WebClient.create()
                    .head().uri(snap).retrieve().toBodilessEntity().block();

            if (head != null && head.getStatusCode().is2xxSuccessful()) {
                String lm = head.getHeaders().getFirst("Last-Modified");
                if (lm != null) {
                    var last = ZonedDateTime.parse(lm, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                    boolean fresh = Instant.now().minusSeconds(maxAgeSec).isBefore(last);
                    return new Probe(fresh, last.toEpochMilli(), fresh?null:"stale", snap);
                }
                return new Probe(true, null, null, snap);
            }
            return new Probe(false, null, "snapshot missing", snap);
        } catch (Exception e) {
            return new Probe(false, null, "snapshot error", null);
        }
    }

    private void sendCameraAlertOnly(Camera c, Probe stat, String reason){
        Map<String,Object> alert = Map.of(
                "cameraId", c.getId(),
                "externalId", c.getExternalId(),
                "lat", c.getLat(),
                "lng", c.getLng(),
                "reason", reason,
                "ts", System.currentTimeMillis()
        );
        ws.convertAndSend("/topic/camera-alerts", alert);

        var dto = new CameraAlertDto(System.currentTimeMillis(), null, c.getId(), c.getName(),
                (c.getPublicUrl()!=null && !c.getPublicUrl().isBlank())?c.getPublicUrl():c.getUrl(),
                c.getLat(), c.getLng());
        sse.publishCameraAlert(dto);
    }

    private void raiseIncidentFromCamera(Camera c, CameraPolicy policy, String reason){
        var e = new Incident();
        e.setExternalId(java.util.UUID.randomUUID().toString());
        e.setObjectId("camera:"+c.getId());
        e.setLevel(policy.getIncidentLevel());
        e.setKind(policy.getIncidentKind());
        e.setReason(reason);
        e.setLat(c.getLat()); e.setLng(c.getLng());
        e.setTs(System.currentTimeMillis());
        e.setRegionCode(c.getRegionCode());
        e.setOriginRegion(c.getRegionCode());
        if (policy.getTtlSec()!=null) e.setTtlSec(policy.getTtlSec());

        final Incident saved = incidents.save(e);

        var dto = new IncidentDto(saved.getId(), saved.getExternalId(), saved.getObjectId(),
                saved.getLevel(), saved.getKind(), saved.getReason(),
                saved.getLat(), saved.getLng(), saved.getTs(), saved.getStatus(),
                saved.getRegionCode(), saved.getOriginRegion());

        sse.publish(dto);
        p2p.broadcastIncidents(java.util.List.of(dto));

        News n = new News();
        n.setTs(saved.getTs());
        n.setTitle("Обнаружена аномалия: " + saved.getKind());
        n.setBody("Уровень: " + saved.getLevel() + ". Источник: камера " + c.getName());
        n.setRegionCode(saved.getRegionCode());
        n.setSource("VISION");
        n.setIncidentExternalId(saved.getExternalId());
        n.setLat(saved.getLat()); n.setLng(saved.getLng());
        newsRepo.save(n);

        var newsDto = new NewsDto(n.getId(), n.getTs(), n.getTitle(), n.getBody(),
                n.getRegionCode(), n.getSource(), n.getIncidentExternalId(),
                n.getPlaceId(), n.getLat(), n.getLng(), n.getStatus());
        sse.publishNews(newsDto);
        p2p.broadcastNews(java.util.List.of(newsDto));

        notificationService.incidentRaised(saved);

        cameraService.publishAlert(saved.getExternalId(), c, saved.getLat(), saved.getLng());
    }

    private static String trimResp(String raw){
        String s = raw.replaceAll("\\s+"," ").trim();
        if (s.length() > 160) s = s.substring(0,157) + "...";
        return s;
    }
}