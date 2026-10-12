package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.exceptions.InvalidRateAmountException;
import com.bgsistemas.api.rate.exceptions.RateRuleNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateRuleServiceImplTest {

    @Mock
    private RateRuleRepository rateRuleRepository;

    @InjectMocks
    private RateRuleServiceImpl rateRuleService;

    private RateRule rule(Long id, AdjustmentType type, String amount) {
        RateRule rule = new RateRule(RateCategory.NUM_LOCATIONS, "TWO_THREE", "2-3 locales",
                new BigDecimal(amount), type);
        rule.setId(id);
        return rule;
    }

    @Test
    void shouldReturnAllRulesOrdered() {
        List<RateRule> rules = List.of(rule(1L, AdjustmentType.FACTOR, "1.80"));
        when(rateRuleRepository.findAllByOrderByCategoryAscIdAsc()).thenReturn(rules);

        assertThat(rateRuleService.getAllRules()).isEqualTo(rules);
    }

    @Test
    void shouldUpdateAmount() {
        RateRule existing = rule(1L, AdjustmentType.FACTOR, "1.80");
        when(rateRuleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(rateRuleRepository.save(existing)).thenReturn(existing);

        RateRule updated = rateRuleService.updateAmount(1L, new BigDecimal("2.00"));

        assertThat(updated.getAmount()).isEqualByComparingTo("2.00");
        verify(rateRuleRepository).save(existing);
    }

    @Test
    void shouldAllowZeroForAnAddition() {
        RateRule existing = rule(2L, AdjustmentType.ADDITION, "10.00");
        when(rateRuleRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(rateRuleRepository.save(existing)).thenReturn(existing);

        assertThat(rateRuleService.updateAmount(2L, BigDecimal.ZERO).getAmount()).isZero();
    }

    @Test
    void shouldRejectZeroFactor() {
        when(rateRuleRepository.findById(1L)).thenReturn(Optional.of(rule(1L, AdjustmentType.FACTOR, "1.80")));

        assertThatThrownBy(() -> rateRuleService.updateAmount(1L, BigDecimal.ZERO))
                .isInstanceOf(InvalidRateAmountException.class)
                .hasMessage("A factor must be greater than 0");
        verify(rateRuleRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRuleDoesNotExist() {
        when(rateRuleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rateRuleService.updateAmount(99L, BigDecimal.ONE))
                .isInstanceOf(RateRuleNotFoundException.class)
                .hasMessage("Rate rule with id 99 not found");
    }
}
