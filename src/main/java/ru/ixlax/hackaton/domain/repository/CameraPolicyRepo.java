package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ixlax.hackaton.domain.entity.CameraPolicy;

import java.util.Optional;

public interface CameraPolicyRepo extends JpaRepository<CameraPolicy, Long> {
    Optional<CameraPolicy> findByCameraId(Long cameraId);
}