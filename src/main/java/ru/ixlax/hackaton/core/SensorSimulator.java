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
        switch ((type+"").toUpperCase()) {
            case "RADIATION" -> {
                if (p < 0.02)  return new Sample(2.1 + rnd.nextDouble()*1.2, "μSv/h"); // ALERT > ~2.0
                if (p < 0.10)  return new Sample(0.6 + rnd.nextDouble()*0.8, "μSv/h"); // WARN  > ~0.5
                return new Sample(0.08 + rnd.nextDouble()*0.30, "μSv/h");
            }
            case "WATER_LEVEL" -> { // 0..1 – нормируемую “высоту”/заполнение
                if (p < 0.02)  return new Sample(0.92 + rnd.nextDouble()*0.08, null); // ALERT >0.9
                if (p < 0.10)  return new Sample(0.55 + rnd.nextDouble()*0.25, null); // WARN  >0.5
                return new Sample(0.02 + rnd.nextDouble()*0.35, null);
            }
            case "LIGHT" -> { // 0..1 – относительная яркость (в реале: lux/линейка)
                if (p < 0.02)  return new Sample(0.95 + rnd.nextDouble()*0.05, null); // ALERT вспышка/всполох
                if (p < 0.10)  return new Sample(0.70 + rnd.nextDouble()*0.20, null); // WARN аномально ярко
                return new Sample(0.10 + rnd.nextDouble()*0.50, null);
            }
            default -> { return new Sample(rnd.nextDouble(), null); }
        }
    }

    private record Sample(double value, String unit) {}
}