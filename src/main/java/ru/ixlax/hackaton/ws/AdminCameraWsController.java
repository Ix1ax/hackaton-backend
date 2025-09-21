package ru.ixlax.hackaton.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import ru.ixlax.hackaton.core.CameraMonitorJob;
import ru.ixlax.hackaton.core.VisionAnalyzer;
import ru.ixlax.hackaton.domain.entity.Camera;
import ru.ixlax.hackaton.domain.repository.CameraRepo;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AdminCameraWsController {

    private final VisionAnalyzer analyzer;
    private final CameraRepo cameras;
    private final CameraMonitorJob job;

    public record DetectPayload(Long id, String externalId, String snapshotUrl, Boolean createIncident) {}

    @MessageMapping("/admin/camera/detect")
    public String detect(DetectPayload in) throws Exception {
        Optional<Camera> camOpt = (in.id()!=null)
                ? cameras.findById(in.id())
                : (in.externalId()!=null ? cameras.findByExternalId(in.externalId()) : Optional.empty());

        Camera c = camOpt.orElseThrow();
        var hit = analyzer.detect(c, null, in.snapshotUrl());
        if (hit.isEmpty()) return "OK";
        return "HIT: " + hit.get();
    }

    @MessageMapping("/admin/camera-ai/scan-now/{cameraId}")
    public Map<String,Object> scanNow(@DestinationVariable Long cameraId) {
        cameras.findById(cameraId).ifPresent(c -> job.scanOnce(cameraId));
        return Map.of("status", "queued");
    }
}