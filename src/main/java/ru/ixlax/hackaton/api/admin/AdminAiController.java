package ru.ixlax.hackaton.api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ixlax.hackaton.core.AiDigestJob;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/ai")
public class AdminAiController {
    private final AiDigestJob job;

    @PostMapping("/run-digest")
    public String runDigest() {
        job.run();
        return "OK";
    }
}