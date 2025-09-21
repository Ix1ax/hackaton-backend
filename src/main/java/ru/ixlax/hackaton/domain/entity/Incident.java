package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentKind;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentLevel;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentStatus;

@Data
@NoArgsConstructor
@Entity
@Table(indexes = {
        @Index(columnList = "ts"),
        @Index(columnList = "regionCode")
})
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String externalId;

    private String objectId;

    @Enumerated(EnumType.STRING)
    private IncidentLevel level;

    @Enumerated(EnumType.STRING)
    private IncidentKind kind;

    private String reason;

    private double lat;
    private double lng;

    private long ts;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status = IncidentStatus.NEW;

    private String regionCode;     // регион, в котором отображается
    private String originRegion;   // регион-источник

    private Integer zoneRadiusM;   // радиус поражения
    private Integer ttlSec;        // время жизни
}