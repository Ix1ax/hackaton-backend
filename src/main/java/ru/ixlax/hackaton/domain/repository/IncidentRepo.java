package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.ixlax.hackaton.domain.entity.Incident;
import ru.ixlax.hackaton.domain.entity.enums.incident.IncidentStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepo extends JpaRepository<Incident, Long> {

    List<Incident> findTop200ByOrderByTsDesc();

    List<Incident> findByTsGreaterThanOrderByTsAsc(long since);

    Optional<Incident> findByExternalId(String externalId);

    @Query("""
           select i from Incident i
           where i.ts > :since
             and i.lat between :minLat and :maxLat
             and i.lng between :minLng and :maxLng
           order by i.ts desc
           """)
    List<Incident> findByBboxSince(long since,
                                   double minLat, double maxLat,
                                   double minLng, double maxLng,
                                   Pageable pageable);

    Page<Incident> findByTsGreaterThanEqualAndLatBetweenAndLngBetween(long s, double minLat, double maxLat, double minLng, double maxLng, Pageable pg);

    Optional<Incident> findTopByObjectIdAndStatusInOrderByTsDesc(String objectId, List<IncidentStatus> statuses);

    @Query("""
           select i from Incident i
           where i.ts >= :s
             and i.lat between :minLat and :maxLat
             and i.lng between :minLng and :maxLng
             and i.status <> ru.ixlax.hackaton.domain.entity.enums.incident.IncidentStatus.RESOLVED
           order by i.ts desc
           """)
    Page<Incident> findActiveByBboxSince(long s, double minLat, double maxLat, double minLng, double maxLng, Pageable pg);

    // для TTL-джоба — активные, у которых проставлен TTL
    List<Incident> findByStatusInAndTtlSecNotNull(List<IncidentStatus> statuses);
}