package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.RateCalculationRequestDTO;
import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RateServiceImpl implements RateService {

    private final RateRuleRepository rateRuleRepository;
    private final RateCalculator rateCalculator;

    public RateServiceImpl(RateRuleRepository rateRuleRepository, RateCalculator rateCalculator) {
        this.rateRuleRepository = rateRuleRepository;
        this.rateCalculator = rateCalculator;
    }

    @Override
    public RateQuoteDTO calculate(RateCalculationRequestDTO answers) {
        // Each answer of the wizard belongs to one category
        Map<RateCategory, String> chosenOptions = Map.of(
                RateCategory.BUSINESS_TYPE, answers.businessType(),
                RateCategory.NUM_LOCATIONS, answers.locations(),
                RateCategory.POS_COUNT, answers.posCount(),
                RateCategory.TERRACE, answers.space(),
                RateCategory.SQUARE_METERS, answers.squareMeters()
        );

        // An answer without a rule is simply left out (BGSI-158)
        List<RateRule> rules = chosenOptions.entrySet().stream()
                .map(entry -> rateRuleRepository.findByCategoryAndOptionCode(entry.getKey(), entry.getValue()))
                .flatMap(Optional::stream)
                .toList();

        return rateCalculator.calculate(rules);
    }
}
