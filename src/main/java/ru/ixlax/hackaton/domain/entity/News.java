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

    private long ts;                   // когда создана
    private String title;              // короткий заголовок
    @Column(length = 4000)
    private String body;               // текст

    private String regionCode;         // для фильтрации ленты по региону
    private String source;             // SENSOR | DISPATCHER

    // привязки
    private String incidentExternalId; // связанный инцидент (строка, чтобы одинаково на всех узлах)
    private Long placeId;              // опционально: ссылка на Place

    // чтобы при клике прыгать на карту, даже если нет инцидента
    private Double lat;
    private Double lng;

    // статус публикации
    private String status = "PUBLISHED"; // PUBLISHED | HIDDEN
}