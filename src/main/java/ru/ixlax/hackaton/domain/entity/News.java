package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(indexes = { @Index(columnList = "ts"), @Index(columnList = "regionCode") })
public class News {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long ts;
    private String title;
    @Column(length = 4000)
    private String body;

    private String regionCode;
    private String source;

    private String incidentExternalId;
    private Long placeId;

    private Double lat;
    private Double lng;

    private String status = "PUBLISHED";
}