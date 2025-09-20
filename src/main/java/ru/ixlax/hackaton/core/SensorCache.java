// src/main/java/ru/ixlax/hackaton/core/SensorCache.java
package ru.ixlax.hackaton.core;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ixlax.hackaton.domain.entity.Sensor;
import ru.ixlax.hackaton.domain.repository.SensorRepo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SensorCache {

    public record Basic(long id, String name, String type, double lat, double lng, String region) {}

    private final SensorRepo repo;
    private final Map<Long, Basic> byId = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadAll() {
        List<Sensor> all = repo.findAll();
        for (Sensor s : all) put(s);
    }

    public void put(Sensor s) {
        byId.put(s.getId(), new Basic(
                s.getId(),
                s.getName(),
                String.valueOf(s.getType()),
                s.getLat(),
                s.getLng(),
                s.getRegionCode()
        ));
    }

    public Basic get(long id) {
        return byId.get(id);
    }
}