package com.bgsistemas.api.rate;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import com.bgsistemas.api.rate.exceptions.IncompleteRateAnswersException;
import com.bgsistemas.api.rate.mappers.RateQuestionMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, RateQuestionMapper.class})
@WebMvcTest(RateController.class)
class RateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateService rateService;

    @MockitoBean
    private RateQuestionService rateQuestionService;

    private static final String ANSWERS_JSON = """
            { "optionIds": [1, 7, 10, 13, 15] }
            """;

    // BGSI-233: the public wizard reads its questions from the API, without prices
    @Test
    void shouldReturnTheStepsWithoutLoginAndWithoutPrices() throws Exception {
        RateQuestion business = new RateQuestion("Negocio", "¿Qué tipo de negocio tienes?", "Elige una opción.", 0);
        business.setId(1L);
        RateOption restaurant = new RateOption("🍽️", "Restaurante", "Con mesas.", new BigDecimal("49.00"), AdjustmentType.BASE);
        restaurant.setId(1L);
        business.addOption(restaurant);
        when(rateQuestionService.getActiveSteps()).thenReturn(List.of(business));

        mockMvc.perform(get("/api/rates/steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Negocio"))
                .andExpect(jsonPath("$[0].options[0].label").value("Restaurante"))
                .andExpect(jsonPath("$[0].options[0].icon").value("🍽️"))
                .andExpect(jsonPath("$[0].options[0].amount").doesNotExist())
                .andExpect(jsonPath("$[0].options[0].adjustmentType").doesNotExist());
    }

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
    void shouldReturn400WhenThereAreNoAnswers() throws Exception {
        mockMvc.perform(post("/api/rates/calculate")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"optionIds\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.optionIds").value("Choose an answer for every question"));

        verify(rateService, never()).calculate(any());
    }

    @Test
    void shouldReturn400WhenAQuestionIsNotAnswered() throws Exception {
        when(rateService.calculate(any()))
                .thenThrow(new IncompleteRateAnswersException("Choose one answer for every question"));

        mockMvc.perform(post("/api/rates/calculate")
                        .contentType(MediaType.APPLICATION_JSON).content(ANSWERS_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choose one answer for every question"));
    }
}
