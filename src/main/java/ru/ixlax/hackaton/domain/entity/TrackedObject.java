package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data; import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class TrackedObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String objectId;
    private double lat;
    private double lng;
    private double speedMps;
    private long lastTs;
}