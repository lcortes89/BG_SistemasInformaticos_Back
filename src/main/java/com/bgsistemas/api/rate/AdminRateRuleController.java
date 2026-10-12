package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.AdminRateRuleDTO;
import com.bgsistemas.api.rate.dtos.RateRuleGroupDTO;
import com.bgsistemas.api.rate.dtos.RateRuleUpdateRequestDTO;
import com.bgsistemas.api.rate.mappers.RateRuleMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Only ADMIN: SecurityConfig protects every /api/admin/** route
@RestController
@RequestMapping("/api/admin/rate-rules")
public class AdminRateRuleController {

    private final RateRuleService rateRuleService;
    private final RateRuleMapper rateRuleMapper;

    public AdminRateRuleController(RateRuleService rateRuleService, RateRuleMapper rateRuleMapper) {
        this.rateRuleService = rateRuleService;
        this.rateRuleMapper = rateRuleMapper;
    }

    // GET /api/admin/rate-rules -> 5 blocks (one per question) with their rules
    @GetMapping
    public List<RateRuleGroupDTO> getRules() {
        return rateRuleMapper.toGroups(rateRuleService.getAllRules());
    }

    // PUT /api/admin/rate-rules/{id}  { "amount": 55.00 } -> the updated rule
    @PutMapping("/{id}")
    public AdminRateRuleDTO updateAmount(@PathVariable Long id,
                                         @Valid @RequestBody RateRuleUpdateRequestDTO request) {
        return rateRuleMapper.toAdminDTO(rateRuleService.updateAmount(id, request.amount()));
    }
}
