package ru.ixlax.hackaton.api.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.domain.entity.Sensor;
import ru.ixlax.hackaton.domain.repository.SensorRepo;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/sensors")
public class PublicSensorsController {
    private final SensorRepo sensors;

    @GetMapping
    public List<Sensor> list(@RequestParam String region){
        return sensors.findByRegionCode(region);
    }

    @GetMapping("/bbox")
    public List<Sensor> bbox(@RequestParam double minLat, @RequestParam double maxLat,
                             @RequestParam double minLng, @RequestParam double maxLng){
        return sensors.findByLatBetweenAndLngBetween(minLat, maxLat, minLng, maxLng);
    }
}