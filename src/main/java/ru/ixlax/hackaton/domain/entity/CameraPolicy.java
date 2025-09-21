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

    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    private SensorMode mode = SensorMode.AUTO;

    private Integer intervalSec = 60;

    @Column(length = 2000)
    private String promptSuffix;

    private String okRegex = "(?i)\\bok\\b";

    private String hitRegex;

    private String model;

    @Enumerated(EnumType.STRING)
    private IncidentKind incidentKind = IncidentKind.UNKNOWN;

    @Enumerated(EnumType.STRING)
    private IncidentLevel incidentLevel = IncidentLevel.MEDIUM;

    private Integer ttlSec;

    @Column(name = "updated_at")
    private long updatedAt;

    @PrePersist @PreUpdate void touch(){ updatedAt = System.currentTimeMillis(); }
}