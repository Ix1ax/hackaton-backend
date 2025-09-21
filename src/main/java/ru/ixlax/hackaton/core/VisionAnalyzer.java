package ru.ixlax.hackaton.core;

import ru.ixlax.hackaton.domain.entity.Camera;

import java.util.Optional;

public interface VisionAnalyzer {
    /**
     * Анализ с настраиваемым промптом и явным snapshotUrl.
     * Возвращает короткую причину срабатывания; empty() — если всё ок/нет данных.
     */
    Optional<String> detect(Camera c, String userPrompt, String snapshotUrl) throws Exception;

    /** Упрощённый дефолт (без кастомного промпта/URL). */
    default Optional<String> detect(Camera c) throws Exception {
        return detect(c, null, null);
    }
}