package com.bgsistemas.api.rate;

import java.math.BigDecimal;
import java.util.List;

public interface RateRuleService {

    List<RateRule> getAllRules();

    RateRule updateAmount(Long id, BigDecimal amount);
}
