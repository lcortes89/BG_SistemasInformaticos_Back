package com.bgsistemas.api.rate;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import com.bgsistemas.api.rate.exceptions.MissingRateRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(RateController.class)
class RateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateService rateService;

    private static final String ANSWERS_JSON = """
            { "businessType": "RESTAURANT", "locations": "TWO_THREE", "posCount": "TWO_THREE",
              "space": "WITH_TERRACE", "squareMeters": "MEDIUM" }
            """;

    @Test
    void shouldCalculateWithoutLogin() throws Exception {
        when(rateService.calculate(any())).thenReturn(new RateQuoteDTO(
                new BigDecimal("123.20"), new BigDecimal("49.00"), new BigDecimal("1.80"), new BigDecimal("35.00")));

        mockMvc.perform(post("/api/rates/calculate")
                        .contentType(MediaType.APPLICATION_JSON).content(ANSWERS_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPrice").value(123.20))
                .andExpect(jsonPath("$.basePrice").value(49.00));
    }

    @Test
    void shouldReturn400WhenAnAnswerIsMissing() throws Exception {
        mockMvc.perform(post("/api/rates/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"businessType\": \"RESTAURANT\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.locations").value("Number of locations is required"));

        verify(rateService, never()).calculate(any());
    }

    @Test
    void shouldReturn400WhenThereIsNoBasePrice() throws Exception {
        when(rateService.calculate(any()))
                .thenThrow(new MissingRateRuleException("There is no base price for the chosen business type"));

        mockMvc.perform(post("/api/rates/calculate")
                        .contentType(MediaType.APPLICATION_JSON).content(ANSWERS_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("There is no base price for the chosen business type"));
    }
}
