package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentKind;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentLevel;
import ru.ixlax.hackaton.domain.entity.enums.sensor.SensorMode;

@Data
@NoArgsConstructor
@Entity
@Table(indexes = {@Index(columnList = "cameraId", unique = true)})
public class CameraPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cameraId;

    /** Включена ли проверка этой камеры */
    private boolean enabled = true;

    /** AUTO — сразу поднимаем инцидент; MANUAL — только алерт оператору */
    @Enumerated(EnumType.STRING)
    private SensorMode mode = SensorMode.AUTO;

    /** Период в секундах между вызовами ИИ по этой камере */
    private Integer intervalSec = 60;

    /** Дополнительные инструкции для ИИ (добавляется к базовому промпту) */
    @Column(length = 2000)
    private String promptSuffix;

    /** Регэксп, который означает “всё ок, игнорировать” (по умолчанию: (?i)\bok\b ) */
    private String okRegex = "(?i)\\bok\\b";

    /** Если задан — требуем совпадение для срабатывания (напр: (?i)зел.*кож) */
    private String hitRegex;

    /** Переопределение модели (опционально), напр. openai/gpt-4o-mini */
    private String model;

    /** Чем помечать инцидент при срабатывании */
    @Enumerated(EnumType.STRING)
    private IncidentKind incidentKind = IncidentKind.UNKNOWN;

    @Enumerated(EnumType.STRING)
    private IncidentLevel incidentLevel = IncidentLevel.MEDIUM;

    /** TTL инцидента, сек (опционально) */
    private Integer ttlSec;

    @Column(name = "updated_at")
    private long updatedAt;

    @PrePersist @PreUpdate void touch(){ updatedAt = System.currentTimeMillis(); }
}