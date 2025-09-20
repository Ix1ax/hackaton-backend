package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(indexes = {
        @Index(columnList="ts"),
        @Index(columnList="sensorId"),
        @Index(columnList="type")
})
public class Measurement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sensorId;
    private String objectId;

    private long ts;

    private Double lat;
    private Double lng;

    private String type;   // RADIATION/SMOKE/AIR_QUALITY/FLOOD...
    @Column(name = "val")          // <-- БЫЛО: private double value;
    private double value;
    private String unit;   // μSv/h, %, AQI, —
}