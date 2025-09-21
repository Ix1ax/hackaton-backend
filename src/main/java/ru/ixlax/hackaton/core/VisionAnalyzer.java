package ru.ixlax.hackaton.core;

import ru.ixlax.hackaton.domain.entity.Camera;

import java.util.Optional;

public interface VisionAnalyzer {

    Optional<String> detect(Camera c, String userPrompt, String snapshotUrl) throws Exception;

    default Optional<String> detect(Camera c) throws Exception {
        return detect(c, null, null);
    }
}