package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.ixlax.hackaton.domain.entity.enums.sensor.SensorMode;

@Data @NoArgsConstructor
@Entity
@Table(indexes = {@Index(columnList = "sensorId", unique = true)})
public class SensorPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sensorId;

    @Enumerated(EnumType.STRING)
    private SensorMode mode = SensorMode.AUTO;   // AUTO | MANUAL

    // Для метрик "чем больше — тем хуже"
    private Double alertAbove;    // value > alertAbove -> ALERT
    private Double warnAbove;     // value > warnAbove  -> WARN
    private Double clearBelow;    // value < clearBelow -> RESOLVE

    private Integer ttlSec;       // опционально: TTL инцидента

    @Column(name = "updated_at")
    private long updatedAt;

    @PrePersist @PreUpdate
    void touch(){ this.updatedAt = System.currentTimeMillis(); }
}