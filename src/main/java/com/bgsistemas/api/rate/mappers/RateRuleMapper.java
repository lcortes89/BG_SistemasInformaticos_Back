package com.bgsistemas.api.rate.mappers;

import com.bgsistemas.api.rate.RateCategory;
import com.bgsistemas.api.rate.RateRule;
import com.bgsistemas.api.rate.dtos.AdminRateRuleDTO;
import com.bgsistemas.api.rate.dtos.RateRuleGroupDTO;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RateRuleMapper {

    // Entity -> admin panel response
    public AdminRateRuleDTO toAdminDTO(RateRule rule) {
        return new AdminRateRuleDTO(
                rule.getId(),
                rule.getCategory(),
                rule.getOptionCode(),
                rule.getLabel(),
                rule.getAmount(),
                rule.getAdjustmentType()
        );
    }

    // List of rules -> one block per question, in the order of the wizard.
    // Questions without rules are left out.
    public List<RateRuleGroupDTO> toGroups(List<RateRule> rules) {
        Map<RateCategory, List<AdminRateRuleDTO>> byCategory = rules.stream()
                .collect(Collectors.groupingBy(RateRule::getCategory,
                        Collectors.mapping(this::toAdminDTO, Collectors.toList())));

        return Arrays.stream(RateCategory.values())
                .filter(byCategory::containsKey)
                .map(category -> new RateRuleGroupDTO(category, byCategory.get(category)))
                .toList();
    }
}
