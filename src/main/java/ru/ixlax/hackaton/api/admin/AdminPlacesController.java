// src/main/java/ru/ixlax/hackaton/api/admin/AdminPlacesController.java
package ru.ixlax.hackaton.api.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.domain.entity.Place;
import ru.ixlax.hackaton.domain.repository.PlaceRepo;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/places")
public class AdminPlacesController {
    private final PlaceRepo places;
    private final ObjectMapper mapper;

    @PostMapping("/import")
    public Map<String,Object> bulkImport(@RequestBody String json) throws Exception {
        var list = mapper.readValue(json, new TypeReference<List<Place>>() {});
        places.saveAll(list);
        return Map.of("imported", list.size());
    }
}