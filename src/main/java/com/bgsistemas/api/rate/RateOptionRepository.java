package com.bgsistemas.api.rate;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateOptionRepository extends JpaRepository<RateOption, Long> {

    // The answers chosen in the wizard, each one with its question
    @EntityGraph(attributePaths = "question")
    List<RateOption> findByIdIn(List<Long> ids);
}
