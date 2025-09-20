package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ixlax.hackaton.domain.entity.Measurement;

import java.util.List;

@Repository
public interface MeasurementRepo extends JpaRepository<Measurement, Long> {

    long countBySensorIdInAndTsGreaterThan(List<Long> sensorIds, long since);
}