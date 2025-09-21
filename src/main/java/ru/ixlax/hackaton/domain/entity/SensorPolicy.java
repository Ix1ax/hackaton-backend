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
    private SensorMode mode = SensorMode.AUTO;

    private Double alertAbove;
    private Double warnAbove;
    private Double clearBelow;

    private Integer ttlSec;

    @Column(name = "updated_at")
    private long updatedAt;

    @PrePersist @PreUpdate
    void touch(){ this.updatedAt = System.currentTimeMillis(); }
}