package ru.ixlax.hackaton.api.publicapi.dto;

import ru.ixlax.hackaton.domain.entity.enums.incident.*;

public record IncidentDto(
        Long id,
        String externalId,
        String objectId,
        IncidentLevel level,
        IncidentKind kind,
        String reason,
        double lat,
        double lng,
        long ts,
        IncidentStatus status,
        String regionCode,
        String originRegion
) {}