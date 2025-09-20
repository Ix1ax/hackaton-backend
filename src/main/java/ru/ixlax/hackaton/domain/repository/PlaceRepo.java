package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ixlax.hackaton.domain.entity.Place;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepo extends JpaRepository<Place, Long> {

    Page<Place> findAll(Pageable pageable);
    Page<Place> findByRegionCode(String regionCode, Pageable pageable);

    Optional<Place> findByExternalId(String externalId);

    List<Place> findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(long since);
}