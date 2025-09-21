package ru.ixlax.hackaton.api.publicapi.dto;

public record SensorPolicyDto(
        Long sensorId,
        String mode,        // AUTO | MANUAL
        Double alertAbove,
        Double warnAbove,
        Double clearBelow,
        Integer ttlSec
) {}