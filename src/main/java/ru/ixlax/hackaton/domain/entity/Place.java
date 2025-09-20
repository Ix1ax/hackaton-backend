// src/main/java/ru/ixlax/hackaton/domain/entity/Place.java
package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data; import lombok.NoArgsConstructor;
import ru.ixlax.hackaton.domain.entity.enums.place.PlaceType;

@Data
@NoArgsConstructor
@Entity
@Table(indexes=@Index(columnList="regionCode"))
public class Place {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PlaceType type;

    @Column(unique = true)
    private String externalId;     // идемпотентность P2P

    private String name;
    private String address;
    private double lat;
    private double lng;
    private Integer capacity;
    private String regionCode;

    @Column(nullable = false)
    private long updatedAt;        // millis: для конфликтов и /sync
}