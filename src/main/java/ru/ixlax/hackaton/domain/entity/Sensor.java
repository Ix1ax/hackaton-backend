package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data; import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(
        name = "sensor",
        indexes = {
                @Index(columnList = "regionCode"),
                @Index(name = "idx_sensor_updated_at", columnList = "updated_at"),
                @Index(name = "idx_sensor_external_id", columnList = "external_id")
        }
)
public class Sensor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    private String name;

    private String type;

    private double lat;
    private double lng;
    private String regionCode;

    private String meta;

    private boolean simulate = true;

    @Column(name = "updated_at")
    private long updatedAt;

    @PrePersist @PreUpdate
    void touch() { this.updatedAt = System.currentTimeMillis(); }
}