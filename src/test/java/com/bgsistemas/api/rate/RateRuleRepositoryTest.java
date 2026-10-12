package com.bgsistemas.api.rate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RateRuleRepositoryTest {

    @Autowired
    private RateRuleRepository rateRuleRepository;

    @Test
    void shouldFindTheRuleOfAnAnswer() {
        rateRuleRepository.saveAll(RateRuleInitializer.defaultRules());

        assertThat(rateRuleRepository.findByCategoryAndOptionCode(RateCategory.BUSINESS_TYPE, "RESTAURANT"))
                .get()
                .extracting(RateRule::getAmount)
                .satisfies(amount -> assertThat(amount).isEqualByComparingTo("49.00"));
        assertThat(rateRuleRepository.findByCategoryAndOptionCode(RateCategory.BUSINESS_TYPE, "NOPE")).isEmpty();
    }

    @Test
    void shouldReturnAllRulesGroupedByQuestion() {
        rateRuleRepository.saveAll(RateRuleInitializer.defaultRules());

        List<RateRule> rules = rateRuleRepository.findAllByOrderByCategoryAscIdAsc();

        assertThat(rules).hasSize(16);
        assertThat(rules.get(0).getCategory()).isEqualTo(RateCategory.BUSINESS_TYPE);
    }
}
