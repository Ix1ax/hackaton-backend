package ru.ixlax.hackaton.api.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.publicapi.dto.CameraDto;
import ru.ixlax.hackaton.domain.repository.CameraRepo;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/cameras")
public class PublicCamerasController {
    private final CameraRepo repo;

    // PublicCamerasController
    @GetMapping
    public List<CameraDto> list(@RequestParam(required=false) String region){
        var list = (region==null || region.isBlank()) ? repo.findAll() : repo.findByRegionCode(region);
        return list.stream().map(c -> {
            String slug = (c.getExternalId()!=null && !c.getExternalId().isBlank()) ? c.getExternalId() : ("cam"+c.getId());
            String path = "/hls/live/" + slug + "/video1_stream.m3u8";
            String url  = (c.getPublicUrl()!=null && c.getPublicUrl().startsWith("/")) ? c.getPublicUrl() : path;
            return new CameraDto(
                    c.getId(), c.getExternalId(), c.getName(), c.getRegionCode(),
                    c.getLat(), c.getLng(), c.getRadiusM(), url, c.getPublicUrl(),
                    c.getSnapshotUrl(), c.getUpdatedAt()
            );
        }).toList();
    }
}