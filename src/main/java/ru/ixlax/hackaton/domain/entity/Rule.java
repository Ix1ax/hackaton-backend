package ru.ixlax.hackaton.domain.entity;

import jakarta.persistence.*;
import lombok.Data; import lombok.NoArgsConstructor;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentLevel;

@Data @NoArgsConstructor
@Entity
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)
    private IncidentLevel level; // какой уровень ставить при срабатывании
    private Double radiationThresh;  // null = не учитываем
    private Double magneticThresh;
    private Double speedThresh;
}