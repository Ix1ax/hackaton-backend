package ru.ixlax.hackaton.api.publicapi.dto;

public record SensorRegisterDto(
        String name,
        String type,
        Double lat, Double lng,
        String region,
        String meta
) {}
