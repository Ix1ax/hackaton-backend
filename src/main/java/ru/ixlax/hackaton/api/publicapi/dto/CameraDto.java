package ru.ixlax.hackaton.api.publicapi.dto;

import ru.ixlax.hackaton.domain.entity.Camera;

public record CameraDto(
        Long id,
        String externalId,
        String name,
        String regionCode,
        double lat,
        double lng,
        Integer radiusM,
        String url,
        String publicUrl,
        String snapshotUrl,
        long updatedAt
) {
    public static CameraDto fromEntity(Camera c) {
        return new CameraDto(
                c.getId(), c.getExternalId(), c.getName(), c.getRegionCode(),
                c.getLat(), c.getLng(), c.getRadiusM(), c.getUrl(), c.getPublicUrl(),
                c.getSnapshotUrl(), c.getUpdatedAt()
        );
    }
}