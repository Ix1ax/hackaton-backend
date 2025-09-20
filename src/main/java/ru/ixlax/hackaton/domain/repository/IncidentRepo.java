package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.ixlax.hackaton.domain.entity.Incident;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepo extends JpaRepository<Incident, Long> {

    // для SyncOnStartup (твой билд падал из-за отсутствия этого метода)
    List<Incident> findTop200ByOrderByTsDesc();

    // для /p2p/sync/incidents
    List<Incident> findByTsGreaterThanOrderByTsAsc(long since);

    Optional<Incident> findByExternalId(String externalId);

    // для /api/public/incidents (bbox + since)
    @Query("""
           select i from Incident i
           where i.ts > :since
             and i.lat between :minLat and :maxLat
             and i.lng between :minLng and :maxLng
           order by i.ts desc
           """)
    List<Incident> findByBboxSince(long since,
                                   double minLat, double maxLat,
                                   double minLng, double maxLng);
}