package ru.ixlax.hackaton.api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ixlax.hackaton.core.CameraMonitorJob;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/camera")
public class AdminCameraMonitorController {

    private final CameraMonitorJob job;

    @PostMapping("/scan")
    public String scanNow() {
        job.tick();
        return "OK";
    }
}