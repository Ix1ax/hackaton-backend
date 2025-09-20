// src/main/java/ru/ixlax/hackaton/api/publicapi/SensorService.java
package ru.ixlax.hackaton.api.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ixlax.hackaton.api.publicapi.dto.*;
import ru.ixlax.hackaton.api.p2p.P2PPublisher;
import ru.ixlax.hackaton.domain.entity.*;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentKind;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentLevel;
import ru.ixlax.hackaton.domain.repository.*;
import ru.ixlax.hackaton.sse.SseHub;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SensorService {
    private final SensorRepo sensors;
    private final MeasurementRepo measurements;
    private final IncidentRepo incidents;
    private final NewsRepo newsRepo;
    private final SseHub sse;
    private final P2PPublisher p2p;

    @Transactional
    public Sensor register(SensorRegisterDto d){
        var s = new Sensor();
        s.setName(d.name());
        s.setType(d.type());
        s.setLat(d.lat());
        s.setLng(d.lng());
        s.setRegionCode(d.region());
        s.setMeta(d.meta());
        return sensors.save(s);
    }

    @Transactional
    public SensorStatusDto push(MeasurementDto d){
        long now = d.ts()==null ? System.currentTimeMillis() : d.ts();
        var s = sensors.findById(d.sensorId()).orElseThrow();

        var m = new Measurement();
        m.setSensorId(s.getId());
        m.setType(d.type());
        m.setValue(d.value());
        m.setUnit(d.unit());
        m.setTs(now);
        measurements.save(m);

        // простые пороговые правила
        var status = evaluateAndMaybeRaise(s, m);

        sse.publishSensor(status);
        return status;
    }

    private SensorStatusDto evaluateAndMaybeRaise(Sensor s, Measurement m){
        SensorHealth health = SensorHealth.OK;
        String msg = "OK";

        switch ((s.getType()+"").toUpperCase()){
            case "RADIATION" -> {
                if (m.getValue() > 2.0) { health = SensorHealth.ALERT;  msg = "Высокий фон!";   raiseIncident(s,m, IncidentLevel.CRITICAL, IncidentKind.RADIATION); }
                else if (m.getValue() > 0.5) { health = SensorHealth.WARN; msg = "Повышенный фон"; }
            }
            case "SMOKE" -> {
                if (m.getValue() > 0.8) { health = SensorHealth.ALERT;  msg = "Дым обнаружен!"; raiseIncident(s,m, IncidentLevel.HIGH, IncidentKind.FIRE); }
                else if (m.getValue() > 0.3) { health = SensorHealth.WARN; msg = "Подозрение на дым"; }
            }
            case "AIR_QUALITY" -> {
                if (m.getValue() > 200) { health = SensorHealth.ALERT; msg = "Плохое качество воздуха"; raiseIncident(s,m, IncidentLevel.HIGH, IncidentKind.CHEMICAL); }
                else if (m.getValue() > 120) { health = SensorHealth.WARN; msg = "Сомнительное качество воздуха"; }
            }
            case "FLOOD" -> {
                if (m.getValue() > 0.9) { health = SensorHealth.ALERT; msg = "Затопление!"; raiseIncident(s,m, IncidentLevel.CRITICAL, IncidentKind.FLOOD); }
                else if (m.getValue() > 0.5) { health = SensorHealth.WARN; msg = "Высокий уровень воды"; }
            }
            default -> {}
        }

        // Авто-новость при ALERT/WARN
        if (health != SensorHealth.OK) {
            var n = new News();
            n.setTs(m.getTs());
            n.setTitle("Сигнал датчика: " + s.getType() + " = " + m.getValue() + " " + m.getUnit());
            n.setBody("Статус: " + health + ". Сообщение: " + msg + ". Датчик: " + s.getName());
            n.setRegionCode(s.getRegionCode());
            n.setSource("SENSOR");
            n.setPlaceId(null);
            n.setLat(s.getLat()); n.setLng(s.getLng());
            newsRepo.save(n);
            sse.publishNews(new ru.ixlax.hackaton.api.publicapi.dto.NewsDto(
                    n.getId(), n.getTs(), n.getTitle(), n.getBody(),
                    n.getRegionCode(), n.getSource(), n.getIncidentExternalId(),
                    n.getPlaceId(), n.getLat(), n.getLng(), n.getStatus()
            ));
            // p2p-рассылка новостей (ниже добавим поддержку)
            // p2p.broadcastNews(List.of(dto));
        }

        return new SensorStatusDto(s.getId(), s.getType(), health, msg, m.getTs());
    }

    private void raiseIncident(Sensor s, Measurement m, IncidentLevel lvl, IncidentKind kind){
        var e = new Incident();
        e.setExternalId(UUID.randomUUID().toString()); // <-- чтобы уникальность не выстрелила
        e.setLevel(lvl);
        e.setKind(kind);
        e.setReason("sensor:"+s.getType());
        e.setLat(s.getLat()); e.setLng(s.getLng());
        e.setTs(m.getTs());
        e.setRegionCode(s.getRegionCode());
        e.setOriginRegion(s.getRegionCode());
        e = incidents.save(e);

        var dto = new ru.ixlax.hackaton.api.publicapi.dto.IncidentDto(
                e.getId(), e.getExternalId(), e.getObjectId(), e.getLevel(), e.getKind(),
                e.getReason(), e.getLat(), e.getLng(), e.getTs(), e.getStatus(),
                e.getRegionCode(), e.getOriginRegion()
        );
        sse.publish(dto);
        p2p.broadcastIncidents(List.of(dto));
    }
}