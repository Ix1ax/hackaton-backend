package ru.ixlax.hackaton.api.publicapi.dto;

public record SensorStatusDto(
        Long sensorId,
        String type,
        SensorHealth health,
        String message,
        Double lat,
        Double lng,
        long ts
) {}
