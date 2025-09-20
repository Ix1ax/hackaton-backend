package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.ixlax.hackaton.domain.entity.Sensor;

import java.util.List;

@Repository
public interface SensorRepo extends JpaRepository<Sensor, Long> {

    List<Sensor> findByRegionCode(String region);

    @Query("""
           select s from Sensor s
           where s.lat between :minLat and :maxLat
             and s.lng between :minLng and :maxLng
           """)
    List<Sensor> findByBbox(double minLat, double maxLat, double minLng, double maxLng);

    List<Sensor> findBySimulateTrue();
}