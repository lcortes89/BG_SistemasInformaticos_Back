package com.bgsistemas.api.rate.dtos;

import com.bgsistemas.api.rate.RateCategory;

import java.util.List;

// One block of the admin panel: a question of the wizard and its answers
public record RateRuleGroupDTO(
        RateCategory category,
        List<AdminRateRuleDTO> rules
) {
}
