// src/main/java/ru/ixlax/hackaton/domain/entity/Sensor.java
package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data; import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(indexes = { @Index(columnList="regionCode") })
public class Sensor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String externalId;     // идемпотентность P2P

    private String name;
    private String type;           // RADIATION|SMOKE|AIR_QUALITY|FLOOD
    private double lat;
    private double lng;
    private String regionCode;

    @Column(length = 2000)
    private String meta;           // произвольное описание

    private boolean simulate = true;

    @Column(nullable = false)
    private long updatedAt;        // millis
}