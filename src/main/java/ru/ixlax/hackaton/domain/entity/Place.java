package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data; import lombok.NoArgsConstructor;
import ru.ixlax.hackaton.domain.entity.enums.place.PlaceType;

@Data
@NoArgsConstructor
@Entity
@Table(
        name = "place",
        indexes = {
                @Index(columnList = "regionCode"),
                @Index(name = "idx_place_updated_at", columnList = "updated_at"),
                @Index(name = "idx_place_external_id", columnList = "external_id")
        }
)
public class Place {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Enumerated(EnumType.STRING)
    private PlaceType type;

    private String name;
    private String address;
    private double lat;
    private double lng;
    private Integer capacity;
    private String regionCode;

    @Column(name = "updated_at")
    private long updatedAt;

    @PrePersist @PreUpdate
    void touch() { this.updatedAt = System.currentTimeMillis(); }
}