package ru.ixlax.hackaton.api.publicapi.dto;

public record MeasurementDto(
        Long sensorId,
        String type,
        double value,
        String unit,
        Long ts
) {}
