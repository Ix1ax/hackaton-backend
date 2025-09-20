// src/main/java/ru/ixlax/hackaton/api/publicapi/dto/PlaceDto.java
package ru.ixlax.hackaton.api.publicapi.dto;

import ru.ixlax.hackaton.domain.entity.enums.place.PlaceType;

public record PlaceDto(
        Long id,
        String externalId,
        PlaceType type,
        String name,
        String address,
        double lat,
        double lng,
        Integer capacity,
        String regionCode,
        long updatedAt
) {}