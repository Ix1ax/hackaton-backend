package ru.ixlax.hackaton.api.publicapi.dto;

public record SensorsSummaryDto(
        String region,
        long total,
        long ok,
        long warn,
        long alert,
        String summaryText,
        long ts
) {}