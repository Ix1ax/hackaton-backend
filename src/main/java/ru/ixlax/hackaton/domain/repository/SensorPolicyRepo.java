package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ixlax.hackaton.domain.entity.SensorPolicy;

import java.util.Optional;

public interface SensorPolicyRepo extends JpaRepository<SensorPolicy, Long> {
    Optional<SensorPolicy> findBySensorId(Long sensorId);
}