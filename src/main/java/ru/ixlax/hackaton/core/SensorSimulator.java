package ru.ixlax.hackaton.core;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ixlax.hackaton.api.publicapi.SensorService;
import ru.ixlax.hackaton.api.publicapi.dto.MeasurementDto;
import ru.ixlax.hackaton.domain.entity.Sensor;
import ru.ixlax.hackaton.domain.repository.SensorRepo;

import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sensors.simulate.enabled", havingValue = "true", matchIfMissing = true)
public class SensorSimulator {

    private final SensorRepo sensors;
    private final SensorService service;
    private final Random rnd = new Random();


    @Scheduled(fixedDelayString = "${app.sensors.simulate.period-ms:60000}")
    public void tick() {
        List<Sensor> list = sensors.findBySimulateTrue();
        long now = System.currentTimeMillis();

        for (Sensor s : list) {
            int n = 1 + rnd.nextInt(3);
            for (int i = 0; i < n; i++) {
                var v = valueFor(s.getType());
                var dto = new MeasurementDto(
                        s.getId(),
                        s.getType(),
                        v.value,
                        v.unit,
                        now - rnd.nextInt(15_000)
                );
                try { service.push(dto); } catch (Exception ignored) {}
            }
        }
    }

    private Sample valueFor(String type) {
        double p = rnd.nextDouble();

        switch (String.valueOf(type).toUpperCase()) {
            case "RADIATION" -> {
                if (p < 0.02)  return new Sample( 1.8 + rnd.nextDouble()*1.2, "μSv/h"); // ALERT ~2.0+
                if (p < 0.10)  return new Sample( 0.6 + rnd.nextDouble()*0.8, "μSv/h"); // WARN  ~0.5–1.4
                return new Sample( 0.08 + rnd.nextDouble()*0.30, "μSv/h");               // OK    ~0.08–0.38
            }
            case "SMOKE" -> {
                if (p < 0.02)  return new Sample( 0.90 + rnd.nextDouble()*0.10, null);   // ALERT >0.9
                if (p < 0.10)  return new Sample( 0.35 + rnd.nextDouble()*0.40, null);   // WARN  ~0.35–0.75
                return new Sample( 0.02 + rnd.nextDouble()*0.20, null);                  // OK    ~0.02–0.22
            }
            case "AIR_QUALITY" -> {
                if (p < 0.02)  return new Sample( 220 + rnd.nextDouble()*60, null);      // ALERT >200
                if (p < 0.10)  return new Sample( 130 + rnd.nextDouble()*60, null);      // WARN  >120
                return new Sample( 45 + rnd.nextDouble()*60, null);                       // OK
            }
            case "FLOOD" -> {
                if (p < 0.02)  return new Sample( 0.92 + rnd.nextDouble()*0.08, null);   // ALERT >0.9
                if (p < 0.10)  return new Sample( 0.55 + rnd.nextDouble()*0.25, null);   // WARN  >0.5
                return new Sample( 0.02 + rnd.nextDouble()*0.35, null);                  // OK
            }
            default -> {
                return new Sample( rnd.nextDouble(), null);
            }
        }
    }

    private record Sample(double value, String unit) {}
}