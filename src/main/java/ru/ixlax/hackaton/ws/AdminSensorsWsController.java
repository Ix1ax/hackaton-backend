package ru.ixlax.hackaton.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.ixlax.hackaton.api.publicapi.SensorService;
import ru.ixlax.hackaton.api.publicapi.dto.MeasurementDto;
import ru.ixlax.hackaton.api.publicapi.dto.SensorPolicyDto;
import ru.ixlax.hackaton.domain.entity.Sensor;
import ru.ixlax.hackaton.domain.entity.SensorPolicy;
import ru.ixlax.hackaton.domain.entity.enums.sensor.SensorMode;
import ru.ixlax.hackaton.domain.repository.SensorPolicyRepo;

@Controller
@RequiredArgsConstructor
public class AdminSensorsWsController {

    private final SensorService sensorService;
    private final SensorPolicyRepo policies;
    private final SimpMessagingTemplate ws;

    @MessageMapping("/admin/sensors/register")
    public void register(ru.ixlax.hackaton.api.publicapi.dto.SensorRegisterDto dto) {
        Sensor saved = sensorService.register(dto);
        ws.convertAndSend("/topic/admin/sensors/registered", saved);
    }

    @MessageMapping("/admin/sensors/push")
    public void push(MeasurementDto dto) {
        var status = sensorService.push(dto);
        ws.convertAndSend("/topic/admin/sensors/status", status);
    }

    @MessageMapping("/admin/sensors/{id}/policy")
    public void upsertPolicy(@DestinationVariable Long id, SensorPolicyDto body) {
        var p = policies.findBySensorId(id).orElseGet(SensorPolicy::new);
        p.setSensorId(id);
        if (body.mode()!=null) p.setMode(SensorMode.valueOf(body.mode().toUpperCase()));
        p.setAlertAbove(body.alertAbove());
        p.setWarnAbove(body.warnAbove());
        p.setClearBelow(body.clearBelow());
        p.setTtlSec(body.ttlSec());
        ws.convertAndSend("/topic/admin/sensors/policy", policies.save(p));
    }
}