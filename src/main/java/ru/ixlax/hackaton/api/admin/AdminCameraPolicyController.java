package ru.ixlax.hackaton.api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.domain.entity.CameraPolicy;
import ru.ixlax.hackaton.domain.repository.CameraPolicyRepo;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/camera-policy")
public class AdminCameraPolicyController {
    private final CameraPolicyRepo repo;

    @GetMapping("/{cameraId}")
    public CameraPolicy get(@PathVariable Long cameraId){
        return repo.findByCameraId(cameraId).orElseGet(() -> {
            var p = new CameraPolicy(); p.setCameraId(cameraId); return p;
        });
    }

    @PostMapping("/{cameraId}")
    public CameraPolicy upsert(@PathVariable Long cameraId, @RequestBody CameraPolicy body){
        var p = repo.findByCameraId(cameraId).orElseGet(CameraPolicy::new);
        p.setCameraId(cameraId);
        if (body.getIntervalSec() != null) p.setIntervalSec(body.getIntervalSec());
        if (body.getPromptSuffix() != null) p.setPromptSuffix(body.getPromptSuffix());
        if (body.getOkRegex() != null) p.setOkRegex(body.getOkRegex());
        if (body.getHitRegex() != null) p.setHitRegex(body.getHitRegex());
        if (body.getModel() != null) p.setModel(body.getModel());
        if (body.getTtlSec() != null) p.setTtlSec(body.getTtlSec());
        if (body.getIncidentKind() != null) p.setIncidentKind(body.getIncidentKind());
        if (body.getIncidentLevel() != null) p.setIncidentLevel(body.getIncidentLevel());
        if (body.getMode() != null) p.setMode(body.getMode());
        p.setEnabled(body.isEnabled());
        return repo.save(p);
    }
}