package ru.ixlax.hackaton.api.publicapi.dto;

public record SensorDto(
        Long id,
        String externalId,
        String name,
        String type,
        double lat,
        double lng,
        String regionCode,
        String meta,
        long updatedAt
) {}