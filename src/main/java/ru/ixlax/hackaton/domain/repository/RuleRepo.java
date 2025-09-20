package ru.ixlax.hackaton.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ixlax.hackaton.domain.entity.Rule;

@Repository
public interface RuleRepo extends JpaRepository<Rule, Long> {
}
