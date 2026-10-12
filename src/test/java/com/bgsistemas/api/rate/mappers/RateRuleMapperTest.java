package com.bgsistemas.api.rate.mappers;

import com.bgsistemas.api.rate.AdjustmentType;
import com.bgsistemas.api.rate.RateCategory;
import com.bgsistemas.api.rate.RateRule;
import com.bgsistemas.api.rate.dtos.AdminRateRuleDTO;
import com.bgsistemas.api.rate.dtos.RateRuleGroupDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateRuleMapperTest {

    private final RateRuleMapper mapper = new RateRuleMapper();

    private RateRule rule(RateCategory category, String code, String amount, AdjustmentType type) {
        return new RateRule(category, code, code, new BigDecimal(amount), type);
    }

    @Test
    void shouldMapRuleToAdminDTO() {
        RateRule rule = new RateRule(RateCategory.BUSINESS_TYPE, "RESTAURANT", "Restaurante",
                new BigDecimal("49.00"), AdjustmentType.BASE);
        rule.setId(1L);

        AdminRateRuleDTO dto = mapper.toAdminDTO(rule);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.category()).isEqualTo(RateCategory.BUSINESS_TYPE);
        assertThat(dto.optionCode()).isEqualTo("RESTAURANT");
        assertThat(dto.label()).isEqualTo("Restaurante");
        assertThat(dto.amount()).isEqualByComparingTo("49.00");
        assertThat(dto.adjustmentType()).isEqualTo(AdjustmentType.BASE);
    }

    @Test
    void shouldGroupRulesInWizardOrder() {
        // Mixed order on purpose: the mapper must sort the blocks like the wizard
        List<RateRule> rules = List.of(
                rule(RateCategory.SQUARE_METERS, "SMALL", "0", AdjustmentType.ADDITION),
                rule(RateCategory.BUSINESS_TYPE, "RESTAURANT", "49", AdjustmentType.BASE),
                rule(RateCategory.NUM_LOCATIONS, "ONE", "1.00", AdjustmentType.FACTOR),
                rule(RateCategory.BUSINESS_TYPE, "BAR_CAFE", "39", AdjustmentType.BASE)
        );

        List<RateRuleGroupDTO> groups = mapper.toGroups(rules);

        assertThat(groups).extracting(RateRuleGroupDTO::category).containsExactly(
                RateCategory.BUSINESS_TYPE, RateCategory.NUM_LOCATIONS, RateCategory.SQUARE_METERS);
        assertThat(groups.get(0).rules()).extracting(AdminRateRuleDTO::optionCode)
                .containsExactly("RESTAURANT", "BAR_CAFE");
    }

    @Test
    void shouldLeaveOutQuestionsWithoutRules() {
        List<RateRuleGroupDTO> groups = mapper.toGroups(
                List.of(rule(RateCategory.TERRACE, "INDOOR", "0", AdjustmentType.ADDITION)));

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).category()).isEqualTo(RateCategory.TERRACE);
    }
}
