package ru.ixlax.hackaton.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.ixlax.hackaton.domain.entity.Camera;

import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.camera.monitor.analyzer", havingValue = "noop", matchIfMissing = true)
public class VisionAnalyzerNoop implements VisionAnalyzer {
    @Override public Optional<String> detect(Camera c, String userPrompt, String snapshotUrl) {
        return Optional.empty();
    }
}