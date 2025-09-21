package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ixlax.hackaton.domain.entity.Camera;

import java.util.List;
import java.util.Optional;

@Repository
public interface CameraRepo extends JpaRepository<Camera, Long> {
    Optional<Camera> findByExternalId(String externalId);
    List<Camera> findByRegionCode(String regionCode);
    List<Camera> findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(long since);
}