package com.bgsistemas.api.rate;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateQuestionRepository extends JpaRepository<RateQuestion, Long> {

    // Admin panel: every question with its options, in the order of the wizard
    @EntityGraph(attributePaths = "options")
    List<RateQuestion> findAllByOrderByPositionAscIdAsc();

    // Public wizard: only the visible questions
    @EntityGraph(attributePaths = "options")
    List<RateQuestion> findByActiveTrueOrderByPositionAscIdAsc();
}
