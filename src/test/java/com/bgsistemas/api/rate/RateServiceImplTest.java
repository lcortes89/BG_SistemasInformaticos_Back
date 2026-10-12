package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.RateCalculationRequestDTO;
import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RateServiceImplTest {

    @Mock
    private RateRuleRepository rateRuleRepository;

    // Real calculator: the test checks the whole calculation with the default rules
    private RateServiceImpl rateService() {
        return new RateServiceImpl(rateRuleRepository, new RateCalculator());
    }

    // The fake repository answers with the default rules of RateRuleInitializer
    private void useDefaultRules() {
        lenient().when(rateRuleRepository.findByCategoryAndOptionCode(any(), anyString()))
                .thenAnswer(invocation -> RateRuleInitializer.defaultRules().stream()
                        .filter(rule -> rule.getCategory() == invocation.getArgument(0)
                                && rule.getOptionCode().equals(invocation.getArgument(1)))
                        .findFirst());
    }

    @Test
    void shouldCalculateTheExampleOfTheDocumentation() {
        useDefaultRules();
        RateCalculationRequestDTO answers =
                new RateCalculationRequestDTO("RESTAURANT", "TWO_THREE", "TWO_THREE", "WITH_TERRACE", "MEDIUM");

        RateQuoteDTO quote = rateService().calculate(answers);

        // 49 x 1.8 + 15 + 10 + 10
        assertThat(quote.monthlyPrice()).isEqualByComparingTo("123.20");
    }

    @Test
    void shouldIgnoreAnswersWithoutRule() {
        useDefaultRules();
        RateCalculationRequestDTO answers =
                new RateCalculationRequestDTO("BAR_CAFE", "UNKNOWN", "ONE", "INDOOR", "UNKNOWN");

        RateQuoteDTO quote = rateService().calculate(answers);

        assertThat(quote.monthlyPrice()).isEqualByComparingTo("39.00");
    }

    @Test
    void shouldFindNoRuleForAnUnknownBusinessType() {
        lenient().when(rateRuleRepository.findByCategoryAndOptionCode(any(), anyString())).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> rateService().calculate(
                        new RateCalculationRequestDTO("SPACESHIP", "ONE", "ONE", "INDOOR", "SMALL")))
                .hasMessageContaining("base price");
    }
}
