package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.exceptions.InvalidRateAmountException;
import com.bgsistemas.api.rate.exceptions.RateRuleNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RateRuleServiceImpl implements RateRuleService {

    private final RateRuleRepository rateRuleRepository;

    public RateRuleServiceImpl(RateRuleRepository rateRuleRepository) {
        this.rateRuleRepository = rateRuleRepository;
    }

    @Override
    public List<RateRule> getAllRules() {
        return rateRuleRepository.findAllByOrderByCategoryAscIdAsc();
    }

    @Override
    public RateRule updateAmount(Long id, BigDecimal amount) {
        RateRule rule = rateRuleRepository.findById(id)
                .orElseThrow(() -> new RateRuleNotFoundException(id));

        // A multiplier of 0 would make every quote 0 €
        if (rule.getAdjustmentType() == AdjustmentType.FACTOR && amount.signum() <= 0) {
            throw new InvalidRateAmountException("A factor must be greater than 0");
        }

        rule.setAmount(amount);
        return rateRuleRepository.save(rule);
    }
}
