package ru.ixlax.hackaton.api.publicapi.dto;

public record CameraStatusDto(
        Long id,
        String externalId,
        boolean online,
        Long lastSeenTs,     // null, если не знаем
        String snapshotUrl,  // пригодится фронту
        String reason        // напр. "snapshot missing" / "stale"
) {}