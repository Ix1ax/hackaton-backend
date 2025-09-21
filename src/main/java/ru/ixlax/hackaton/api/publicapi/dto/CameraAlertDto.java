package ru.ixlax.hackaton.api.publicapi.dto;

public record CameraAlertDto(
        long ts,
        String incidentExternalId,
        Long cameraId,
        String cameraName,
        String streamUrl,   // использовать publicUrl
        double lat,
        double lng
) {}