package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ixlax.hackaton.domain.entity.TrackedObject;

@Repository
public interface TrackedObjectRepo extends JpaRepository<TrackedObject, Long> {
}
