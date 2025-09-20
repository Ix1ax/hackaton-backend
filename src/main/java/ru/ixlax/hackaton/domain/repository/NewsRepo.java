package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ixlax.hackaton.domain.entity.News;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepo extends JpaRepository<News, Long> {
    List<News> findTop200ByRegionCodeOrderByTsDesc(String region);
    List<News> findByTsGreaterThanOrderByTsAsc(long since);
    Optional<News> findFirstByTsAndSourceAndTitle(long ts, String source, String title);
    Page<News> findAll(Pageable pageable);
    Page<News> findByRegionCodeOrderByTsDesc(String regionCode, Pageable pageable);
}