package ru.ixlax.hackaton.api.publicapi.dto;

public record CameraAlertDto(
        long ts,
        String incidentExternalId,
        Long cameraId,
        String cameraName,
        String streamUrl,
        double lat,
        double lng
) {}