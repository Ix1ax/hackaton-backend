package ru.ixlax.hackaton.api.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import ru.ixlax.hackaton.api.publicapi.dto.CameraStatusDto;
import ru.ixlax.hackaton.domain.entity.Camera;
import ru.ixlax.hackaton.domain.repository.CameraRepo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cameras/public")
public class CameraStatusController {

    private final CameraRepo cameraRepo;

    @Value("${app.camera.monitor.snapshot-base:http://gateway/snapshots/}")
    String snapshotBase;

    @Value("${app.camera.monitor.snapshot-max-age-sec:600}")
    long maxAgeSec;

    @GetMapping("/status")
    public List<CameraStatusDto> listStatuses() {
        WebClient web = WebClient.builder().build();
        List<Camera> cams = cameraRepo.findAll();
        List<CameraStatusDto> out = new ArrayList<>(cams.size());

        for (Camera c : cams) {
            String slug = (c.getExternalId() != null && !c.getExternalId().isBlank())
                    ? c.getExternalId() : ("cam" + c.getId());
            String snapshotUrl = snapshotBase + URLEncoder.encode(slug + ".jpg", StandardCharsets.UTF_8);

            boolean online = false;
            Long lastSeenTs = null;
            String reason = null;

            try {
                ResponseEntity<Void> head = web.head().uri(snapshotUrl)
                        .retrieve().toBodilessEntity().block();

                if (head != null && head.getStatusCode().is2xxSuccessful()) {
                    String lm = head.getHeaders().getFirst("Last-Modified");
                    if (lm != null) {
                        Instant last = ZonedDateTime.parse(lm, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                        lastSeenTs = last.toEpochMilli();
                        online = Instant.now().minusSeconds(maxAgeSec).isBefore(last);
                        if (!online) reason = "stale";
                    } else {
                        online = true;
                    }
                } else {
                    reason = "snapshot missing";
                }
            } catch (Exception e) {
                reason = "snapshot error";
            }

            out.add(new CameraStatusDto(
                    c.getId(),
                    c.getExternalId(),
                    online,
                    lastSeenTs,
                    snapshotUrl,
                    reason
            ));
        }

        return out;
    }
}