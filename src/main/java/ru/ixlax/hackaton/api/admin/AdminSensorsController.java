package ru.ixlax.hackaton.api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.publicapi.SensorService;
import ru.ixlax.hackaton.api.publicapi.dto.MeasurementDto;
import ru.ixlax.hackaton.api.publicapi.dto.SensorPolicyDto;
import ru.ixlax.hackaton.api.publicapi.dto.SensorStatusDto;
import ru.ixlax.hackaton.api.publicapi.dto.SensorHealth;
import ru.ixlax.hackaton.core.SensorCache;
import ru.ixlax.hackaton.domain.entity.Sensor;
import ru.ixlax.hackaton.domain.entity.SensorPolicy;
import ru.ixlax.hackaton.domain.entity.enums.sensor.SensorMode;
import ru.ixlax.hackaton.domain.repository.SensorPolicyRepo;
import ru.ixlax.hackaton.sse.SseHub;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sensors")
public class AdminSensorsController {
    private final SensorService sensorService;
    private final SensorPolicyRepo policies;

    private final SimpMessagingTemplate ws;
    private final SseHub sse;
    private final SensorCache cache;

    @PostMapping("/register")
    public Sensor register(@RequestBody ru.ixlax.hackaton.api.publicapi.dto.SensorRegisterDto dto){
        Sensor saved = sensorService.register(dto);

        try { cache.put(saved); } catch (Exception ignore) {}

        try { ws.convertAndSend("/topic/admin/sensors/registered", saved); } catch (Exception ignore) {}

        try {
            sse.publishSensor(new SensorStatusDto(
                    saved.getId(),
                    String.valueOf(saved.getType()),
                    SensorHealth.OK,
                    "Registered",
                    dto.lat(),
                    dto.lng(),
                    System.currentTimeMillis(),
                    dto.name(),
                    dto.region(),
                    true

            ));
        } catch (Exception ignore) {}

        return saved;
    }

    @PostMapping("/push")
    public SensorStatusDto push(@RequestBody MeasurementDto dto){
        SensorStatusDto status = sensorService.push(dto);
        try { ws.convertAndSend("/topic/admin/sensors/status", status); } catch (Exception ignore) {}
        return status;
    }

    @GetMapping("/{id}/policy")
    public SensorPolicy getPolicy(@PathVariable Long id) {
        return policies.findBySensorId(id).orElseGet(() -> {
            var p = new SensorPolicy(); p.setSensorId(id); return p;
        });
    }

    @PostMapping("/{id}/policy")
    public SensorPolicy upsertPolicy(@PathVariable Long id, @RequestBody SensorPolicyDto dto){
        var p = policies.findBySensorId(id).orElseGet(SensorPolicy::new);
        p.setSensorId(id);
        if (dto.mode()!=null) p.setMode(SensorMode.valueOf(dto.mode().toUpperCase()));
        p.setAlertAbove(dto.alertAbove());
        p.setWarnAbove(dto.warnAbove());
        p.setClearBelow(dto.clearBelow());
        p.setTtlSec(dto.ttlSec());
        return policies.save(p);
    }
}