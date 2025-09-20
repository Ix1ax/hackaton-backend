package ru.ixlax.hackaton.api.publicapi.dto;

public record NewsDto(
        Long id,
        long ts,
        String title,
        String body,
        String regionCode,
        String source,
        String incidentExternalId,
        Long placeId,
        Double lat,
        Double lng,
        String status
) {}