package com.bgsistemas.api.rate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RateRuleRepository extends JpaRepository<RateRule, Long> {

    // The rule of one answer, e.g. (BUSINESS_TYPE, "RESTAURANT")
    Optional<RateRule> findByCategoryAndOptionCode(RateCategory category, String optionCode);

    // All rules grouped by question (for the admin panel)
    List<RateRule> findAllByOrderByCategoryAscIdAsc();
}
