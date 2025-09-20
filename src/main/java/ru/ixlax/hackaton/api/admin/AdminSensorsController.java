package ru.ixlax.hackaton.api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ixlax.hackaton.api.publicapi.SensorService;
import ru.ixlax.hackaton.api.publicapi.dto.*;
import ru.ixlax.hackaton.domain.entity.Sensor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sensors")
public class AdminSensorsController {
    private final SensorService svc;

    @PostMapping("/register")
    public Sensor register(@RequestBody SensorRegisterDto dto){
        return svc.register(dto);
    }

    @PostMapping("/push")
    public SensorStatusDto push(@RequestBody MeasurementDto dto){
        return svc.push(dto);
    }
}