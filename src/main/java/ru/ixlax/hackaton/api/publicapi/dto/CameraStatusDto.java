package ru.ixlax.hackaton.api.publicapi.dto;

public record CameraStatusDto(
        Long id,
        String externalId,
        boolean online,
        Long lastSeenTs,
        String snapshotUrl,
        String reason
) {}