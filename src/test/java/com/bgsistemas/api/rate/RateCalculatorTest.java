package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import com.bgsistemas.api.rate.exceptions.MissingRateRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Pure unit test: no Spring, no database (BGSI-162)
class RateCalculatorTest {

    private final RateCalculator calculator = new RateCalculator();

    private RateRule rule(String amount, AdjustmentType type) {
        return new RateRule(RateCategory.BUSINESS_TYPE, "ANY", "Any", new BigDecimal(amount), type);
    }

    // price = base x factor + posCount + space + squareMeters
    @ParameterizedTest(name = "{0} x {1} + {2} + {3} + {4} = {5}")
    @CsvSource({
            "49.00, 1.00,  0.00,  0.00,  0.00,  49.00",
            "49.00, 1.80, 15.00, 10.00, 10.00, 123.20",
            "39.00, 2.50, 30.00, 10.00, 20.00, 157.50",
            "35.00, 1.00, 15.00,  0.00,  0.00,  50.00",
            "45.00, 1.80,  0.00, 10.00, 20.00, 111.00"
    })
    void shouldCalculateTheMonthlyPrice(String base, String factor, String pos, String space,
                                        String meters, String expected) {
        List<RateRule> rules = List.of(
                rule(base, AdjustmentType.BASE),
                rule(factor, AdjustmentType.FACTOR),
                rule(pos, AdjustmentType.ADDITION),
                rule(space, AdjustmentType.ADDITION),
                rule(meters, AdjustmentType.ADDITION));

        RateQuoteDTO quote = calculator.calculate(rules);

        assertThat(quote.monthlyPrice()).isEqualByComparingTo(expected);
    }

    @Test
    void shouldReturnTheBreakdownOfThePrice() {
        RateQuoteDTO quote = calculator.calculate(List.of(
                rule("49", AdjustmentType.BASE), rule("1.8", AdjustmentType.FACTOR),
                rule("15", AdjustmentType.ADDITION), rule("10", AdjustmentType.ADDITION)));

        assertThat(quote.basePrice()).isEqualByComparingTo("49.00");
        assertThat(quote.factor()).isEqualByComparingTo("1.80");
        assertThat(quote.additions()).isEqualByComparingTo("25.00");
        assertThat(quote.monthlyPrice()).isEqualByComparingTo("113.20");
    }

    // BGSI-158: an answer without rule does not break the calculation
    @Test
    void shouldUseFactorOneAndZeroAdditionsWhenThoseRulesAreMissing() {
        RateQuoteDTO quote = calculator.calculate(List.of(rule("39", AdjustmentType.BASE)));

        assertThat(quote.factor()).isEqualByComparingTo("1.00");
        assertThat(quote.additions()).isEqualByComparingTo("0.00");
        assertThat(quote.monthlyPrice()).isEqualByComparingTo("39.00");
    }

    @Test
    void shouldFailWhenThereIsNoBasePrice() {
        List<RateRule> onlyAdditions = List.of(rule("10", AdjustmentType.ADDITION));

        assertThatThrownBy(() -> calculator.calculate(onlyAdditions))
                .isInstanceOf(MissingRateRuleException.class)
                .hasMessageContaining("base price");
    }
}
