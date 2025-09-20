package ru.ixlax.hackaton.core;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.ixlax.hackaton.domain.entity.Sensor;
import ru.ixlax.hackaton.domain.repository.SensorRepo;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SensorsBootstrap implements CommandLineRunner {
    private final SensorRepo sensors;

    @Override public void run(String... args) {
        if (sensors.count() > 0) return;

        // простые центры регионов (условные)
        record C(String code, double lat, double lng){}

        var centers = List.of(
                new C("RU-MOW", 55.7558, 37.6173),
                new C("RU-TVE", 56.8586, 35.9176),
                new C("RU-SMO", 54.7818, 32.0401)
        );

        String[] types = {"RADIATION","SMOKE","AIR_QUALITY","FLOOD"};

        long id = 1;
        for (var c : centers) {
            for (int i=0; i<6; i++) {        // по 6*4=24 сенсора на регион
                for (String t : types) {
                    var s = new Sensor();
                    s.setName(t + " #" + (id++));
                    s.setType(t);
                    s.setRegionCode(c.code());
                    s.setLat(c.lat() + (Math.random()-0.5)*0.2);
                    s.setLng(c.lng() + (Math.random()-0.5)*0.2);
                    s.setMeta("{}");
                    s.setSimulate(true);
                    sensors.save(s);
                }
            }
        }
    }
}