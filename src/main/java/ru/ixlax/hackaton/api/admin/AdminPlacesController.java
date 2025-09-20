package ru.ixlax.hackaton.api.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.p2p.P2PPublisher;
import ru.ixlax.hackaton.api.publicapi.dto.PlaceDto;
import ru.ixlax.hackaton.domain.entity.Place;
import ru.ixlax.hackaton.domain.repository.PlaceRepo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/places")
public class AdminPlacesController {

    private final PlaceRepo places;
    private final ObjectMapper mapper;
    private final P2PPublisher p2p;

    @PostMapping("/import")
    public Map<String,Object> bulkImport(@RequestBody String json) throws Exception {
        var list = mapper.readValue(json, new TypeReference<List<Place>>() {});
        long now = System.currentTimeMillis();

        list.forEach(p -> {
            if (p.getExternalId()==null || p.getExternalId().isBlank()) {
                p.setExternalId(UUID.randomUUID().toString());
            }
            p.setUpdatedAt(now);
        });

        var saved = places.saveAll(list);

        p2p.broadcastPlaces(saved.stream().map(this::toDto).toList());

        return Map.of("imported", saved.size());
    }

    private PlaceDto toDto(Place p){
        return new PlaceDto(
                p.getId(), p.getExternalId(), p.getType(), p.getName(), p.getAddress(),
                p.getLat(), p.getLng(), p.getCapacity(), p.getRegionCode(), p.getUpdatedAt()
        );
    }
}