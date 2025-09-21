package ru.ixlax.hackaton.api.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ixlax.hackaton.api.p2p.P2PPublisher;
import ru.ixlax.hackaton.api.publicapi.dto.*;
import ru.ixlax.hackaton.core.CameraService;
import ru.ixlax.hackaton.core.NotificationService;
import ru.ixlax.hackaton.core.SensorCache;
import ru.ixlax.hackaton.domain.entity.*;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentKind;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentLevel;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentStatus;
import ru.ixlax.hackaton.domain.entity.enums.sensor.SensorMode;
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
    private final SensorCache sensorCache;
    private final CameraService cameraService;
    private final SensorPolicyRepo policies;
    private final NotificationService notificationService;

    @Transactional
    public Sensor register(SensorRegisterDto d){
        String type = normalizeType(d.type());

        if (type == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Разрешенные типы сенсоров: RADIATION, WATER_LEVEL, LIGHT"
            );


        var s = new Sensor();
        s.setName(nonEmpty(d.name(), humanNameFor(type)));
        s.setType(type);
        s.setLat(d.lat());
        s.setLng(d.lng());
        s.setRegionCode(d.region());
        s.setMeta(d.meta());
        s.setSimulate(true);

        if (s.getExternalId()==null) s.setExternalId(UUID.randomUUID().toString());
        s.setUpdatedAt(System.currentTimeMillis());

        s = sensors.save(s);
        sensorCache.put(s);
        p2p.broadcastSensors(List.of(toDto(s)));
        return s;
    }

    private static String nonEmpty(String v, String def){ return (v==null || v.isBlank()) ? def : v; }

    private static String humanNameFor(String type){
        return switch (type) {
            case "RADIATION"   -> "Датчик радиации";
            case "WATER_LEVEL" -> "Датчик уровня воды";
            case "LIGHT"       -> "Датчик освещённости";
            default -> "Датчик";
        };
    }

    private static String normalizeType(String raw){
        if (raw == null) return null;
        String t = raw.trim().toUpperCase();
        return switch (t) {
            case "RADIATION", "РАДИАЦИЯ" -> "RADIATION";
            case "WATER_LEVEL", "WATER-LEVEL", "WATERLEVEL", "УРОВЕНЬ_ВОДЫ", "УРОВЕНЬ ВОДЫ" -> "WATER_LEVEL";
            case "LIGHT", "СВЕТ", "ОСВЕЩЕННОСТЬ", "ОСВЕЩЁННОСТЬ" -> "LIGHT";
            default -> null;
        };
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

        s.setUpdatedAt(now);
        sensors.save(s);

        var status = evaluateAndMaybeRaiseOrResolve(s, m);
        sse.publishSensor(status);
        return status;
    }

    private record Eval(SensorHealth health, String message,
                        IncidentLevel levelForIncident, IncidentKind kindForIncident){}

    private Eval evaluateHealth(Sensor s, Measurement m, SensorPolicy policy){
        SensorHealth health = SensorHealth.OK;
        String msg = "OK";

        if (policy != null && policy.getAlertAbove()!=null) {
            if (m.getValue() > policy.getAlertAbove()) { health = SensorHealth.ALERT; msg="Превышение порога"; }
            else if (policy.getWarnAbove()!=null && m.getValue()>policy.getWarnAbove()) { health=SensorHealth.WARN; msg="Повышенный уровень"; }
        } else {
            switch ((s.getType()+"").toUpperCase()){
                case "RADIATION" -> {
                    if (m.getValue() > 2.0)   { health = SensorHealth.ALERT; msg="Высокий радиационный фон"; }
                    else if (m.getValue() > .5){ health = SensorHealth.WARN;  msg="Повышенный радиационный фон"; }
                }
                case "WATER_LEVEL" -> {
                    if (m.getValue() > 0.9)   { health = SensorHealth.ALERT; msg="Затопление"; }
                    else if (m.getValue() > .5){ health = SensorHealth.WARN;  msg="Высокий уровень воды"; }
                }
                case "LIGHT" -> {
                    if (m.getValue() > 0.9)   { health = SensorHealth.ALERT; msg="Аномальная вспышка света"; }
                    else if (m.getValue() > .7){ health = SensorHealth.WARN;  msg="Повышенная освещённость"; }
                }
                default -> {}
            }
        }

        IncidentLevel lvl; IncidentKind kind;
        switch ((s.getType()+"").toUpperCase()){
            case "RADIATION"   -> { lvl = IncidentLevel.HIGH;   kind = IncidentKind.RADIATION_BURST; }
            case "WATER_LEVEL" -> { lvl = IncidentLevel.HIGH;   kind = IncidentKind.FLOOD; }
            case "LIGHT"       -> { lvl = IncidentLevel.MEDIUM; kind = IncidentKind.FIRE; }
            default            -> { lvl = IncidentLevel.LOW;    kind = IncidentKind.UNKNOWN; }
        }
        return new Eval(health, msg, lvl, kind);
    }

    private SensorStatusDto evaluateAndMaybeRaiseOrResolve(Sensor s, Measurement m){
        var policy = policies.findBySensorId(s.getId()).orElse(null);
        var eval = evaluateHealth(s, m, policy);

        var active = incidents.findTopByObjectIdAndStatusInOrderByTsDesc(
                "sensor:"+s.getId(),
                java.util.List.of(IncidentStatus.NEW, IncidentStatus.CONFIRMED)
        ).orElse(null);

        boolean shouldClear = eval.health() == SensorHealth.OK && active != null
                && (policy == null || policy.getClearBelow() == null || m.getValue() < policy.getClearBelow());

        if (shouldClear) {
            active.setStatus(IncidentStatus.RESOLVED);
            incidents.save(active);
            var dto = new IncidentDto(
                    active.getId(), active.getExternalId(), active.getObjectId(),
                    active.getLevel(), active.getKind(), active.getReason(),
                    active.getLat(), active.getLng(), active.getTs(), active.getStatus(),
                    active.getRegionCode(), active.getOriginRegion()
            );
            sse.publish(dto);
            p2p.broadcastIncidents(java.util.List.of(dto));
        }

        boolean shouldAlert = eval.health() == SensorHealth.ALERT && active == null;

        if (shouldAlert) {
            var mode = (policy == null) ? SensorMode.AUTO : policy.getMode();
            if (mode == SensorMode.AUTO) {
                raiseIncident(s, m, eval.levelForIncident(), eval.kindForIncident());
            } else {
                var manual = new SensorStatusDto(s.getId(), s.getType(), SensorHealth.WARN,
                        "Порог превышен — требуется подтверждение оператора", s.getLat(), s.getLng(), m.getTs(), s.getName(),
                        s.getRegionCode(),
                        true);
                sse.publishSensor(manual);
            }
        }

        return new SensorStatusDto(s.getId(), s.getType(), eval.health(), eval.message(), s.getLat(), s.getLng(), m.getTs(), s.getName(), s.getRegionCode(), true);
    }

    private void raiseIncident(Sensor s, Measurement m, IncidentLevel lvl, IncidentKind kind){
        var e = new Incident();
        e.setExternalId(UUID.randomUUID().toString());
        e.setObjectId("sensor:"+s.getId());
        e.setLevel(lvl);
        e.setKind(kind);
        e.setReason("sensor:"+s.getType());
        e.setLat(s.getLat()); e.setLng(s.getLng());
        e.setTs(m.getTs());
        e.setRegionCode(s.getRegionCode());
        e.setOriginRegion(s.getRegionCode());

        policies.findBySensorId(s.getId()).ifPresent(p -> {
            if (p.getTtlSec()!=null) e.setTtlSec(p.getTtlSec());
        });

        final Incident saved = incidents.save(e);

        var dto = new IncidentDto(
                saved.getId(), saved.getExternalId(), saved.getObjectId(), saved.getLevel(), saved.getKind(),
                saved.getReason(), saved.getLat(), saved.getLng(), saved.getTs(), saved.getStatus(),
                saved.getRegionCode(), saved.getOriginRegion()
        );
        sse.publish(dto);
        p2p.broadcastIncidents(List.of(dto));

        // АВТО-НОВОСТЬ для любого инцидента, созданного датчиком
        News n = new News();
        n.setTs(saved.getTs());
        n.setTitle("Обнаружена аномалия: " + saved.getKind());
        n.setBody("Уровень: " + saved.getLevel() + ". Источник: датчик " + s.getType());
        n.setRegionCode(saved.getRegionCode());
        n.setSource("SENSOR");
        n.setIncidentExternalId(saved.getExternalId());
        n.setLat(saved.getLat()); n.setLng(saved.getLng());
        newsRepo.save(n);

        var newsDto = new NewsDto(n.getId(), n.getTs(), n.getTitle(), n.getBody(),
                n.getRegionCode(), n.getSource(), n.getIncidentExternalId(),
                n.getPlaceId(), n.getLat(), n.getLng(), n.getStatus());
        sse.publishNews(newsDto);
        p2p.broadcastNews(List.of(newsDto));

        notificationService.incidentRaised(saved);

        final String extId = saved.getExternalId();
        final double lat = saved.getLat();
        final double lng = saved.getLng();
        final String region = saved.getRegionCode();

        cameraService.pickFor(lat, lng, region)
                .ifPresent(cam -> cameraService.publishAlert(extId, cam, lat, lng));
    }

    private SensorDto toDto(Sensor s){
        return new SensorDto(
                s.getId(), s.getExternalId(), s.getName(), s.getType(),
                s.getLat(), s.getLng(), s.getRegionCode(), s.getMeta(), s.getUpdatedAt()
        );
    }
}