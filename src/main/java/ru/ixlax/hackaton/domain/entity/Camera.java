package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data; import lombok.NoArgsConstructor;

@Data @NoArgsConstructor
@Entity
@Table(indexes = {
        @Index(columnList="regionCode"),
        @Index(name="idx_cam_external_id", columnList="external_id"),
        @Index(name="idx_cam_updated_at", columnList="updated_at")
})
public class Camera {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="external_id", unique=true)
    private String externalId;

    private String name;
    private String regionCode;
    private double lat;
    private double lng;

    /** Радиус зоны покрытия в метрах (простая окружность) */
    private Integer radiusM = 300;

    /** Оригинальный адрес источника (RTSP/HTTP/MJPEG/HLS) */
    private String url;

    /** Публичная HLS/WebRTC/MJPEG ссылка, доступная из браузера через шлюз */
    private String publicUrl;

    /** Ссылка на статичный кадр (jpeg/png), для vision-анализа камер */
    private String snapshotUrl;

    @Column(name="updated_at")
    private long updatedAt;

    @PrePersist @PreUpdate
    void touch(){ this.updatedAt = System.currentTimeMillis(); }
}