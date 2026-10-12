package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import com.bgsistemas.api.rate.exceptions.MissingRateRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// Pure logic: receives the rules of the chosen answers and returns the price.
// It does not touch the database, so it is tested without Spring (BGSI-148).
@Component
public class RateCalculator {

    public RateQuoteDTO calculate(List<RateRule> rules) {
        List<RateRule> baseRules = rules.stream()
                .filter(rule -> rule.getAdjustmentType() == AdjustmentType.BASE)
                .toList();
        if (baseRules.isEmpty()) {
            throw new MissingRateRuleException("There is no base price for the chosen business type");
        }

        BigDecimal base = sum(baseRules);

        // No factor rule -> x1 ; no addition rule -> +0 (BGSI-158)
        BigDecimal factor = rules.stream()
                .filter(rule -> rule.getAdjustmentType() == AdjustmentType.FACTOR)
                .map(RateRule::getAmount)
                .reduce(BigDecimal.ONE, BigDecimal::multiply);

        BigDecimal additions = sum(rules.stream()
                .filter(rule -> rule.getAdjustmentType() == AdjustmentType.ADDITION)
                .toList());

        BigDecimal monthlyPrice = base.multiply(factor).add(additions).setScale(2, RoundingMode.HALF_UP);

        return new RateQuoteDTO(
                monthlyPrice,
                base.setScale(2, RoundingMode.HALF_UP),
                factor.setScale(2, RoundingMode.HALF_UP),
                additions.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal sum(List<RateRule> rules) {
        return rules.stream().map(RateRule::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
