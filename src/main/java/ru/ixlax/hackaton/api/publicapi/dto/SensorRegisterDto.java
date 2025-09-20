package ru.ixlax.hackaton.api.publicapi.dto;

public record SensorRegisterDto(
        String name,
        String type,              // например: RADIATION / SMOKE / FLOOD / AIR_QUALITY
        Double lat, Double lng,
        String region,
        String meta
) {}
