package com.bgsistemas.api.rate;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.rate.exceptions.InvalidRateAmountException;
import com.bgsistemas.api.rate.exceptions.RateRuleNotFoundException;
import com.bgsistemas.api.rate.mappers.RateRuleMapper;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The real RateRuleMapper is used, so the JSON is checked end to end
@Import({SecurityConfig.class, RateRuleMapper.class})
@WebMvcTest(AdminRateRuleController.class)
class AdminRateRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateRuleService rateRuleService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private RateRule restaurant(String amount) {
        RateRule rule = new RateRule(RateCategory.BUSINESS_TYPE, "RESTAURANT", "Restaurante",
                new BigDecimal(amount), AdjustmentType.BASE);
        rule.setId(1L);
        return rule;
    }

    @Test
    void shouldReturnRulesGroupedByQuestion() throws Exception {
        when(rateRuleService.getAllRules()).thenReturn(List.of(restaurant("49.00")));

        mockMvc.perform(get("/api/admin/rate-rules").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("BUSINESS_TYPE"))
                .andExpect(jsonPath("$[0].rules[0].optionCode").value("RESTAURANT"))
                .andExpect(jsonPath("$[0].rules[0].amount").value(49.00))
                .andExpect(jsonPath("$[0].rules[0].adjustmentType").value("BASE"));
    }

    @Test
    void shouldUpdateAmount() throws Exception {
        when(rateRuleService.updateAmount(eq(1L), any(BigDecimal.class))).thenReturn(restaurant("55.00"));

        mockMvc.perform(put("/api/admin/rate-rules/1").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"amount\": 55.00 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(55.00));
    }

    @Test
    void shouldReturn400WhenAmountIsMissing() throws Exception {
        mockMvc.perform(put("/api/admin/rate-rules/1").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").value("Amount is required"));

        verify(rateRuleService, never()).updateAmount(any(), any());
    }

    @Test
    void shouldReturn400WhenAmountIsNegative() throws Exception {
        mockMvc.perform(put("/api/admin/rate-rules/1").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"amount\": -5 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").value("Amount cannot be negative"));
    }

    @Test
    void shouldReturn400WhenFactorIsZero() throws Exception {
        when(rateRuleService.updateAmount(eq(2L), any(BigDecimal.class)))
                .thenThrow(new InvalidRateAmountException("A factor must be greater than 0"));

        mockMvc.perform(put("/api/admin/rate-rules/2").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"amount\": 0 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A factor must be greater than 0"));
    }

    @Test
    void shouldReturn404WhenRuleDoesNotExist() throws Exception {
        when(rateRuleService.updateAmount(eq(99L), any(BigDecimal.class)))
                .thenThrow(new RateRuleNotFoundException(99L));

        mockMvc.perform(put("/api/admin/rate-rules/99").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"amount\": 10 }"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(get("/api/admin/rate-rules"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/rate-rules").with(user("visitor@mail.com").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
