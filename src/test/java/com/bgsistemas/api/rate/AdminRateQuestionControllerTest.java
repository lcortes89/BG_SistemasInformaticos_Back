package com.bgsistemas.api.rate;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.rate.exceptions.InvalidRateAmountException;
import com.bgsistemas.api.rate.exceptions.RateOptionNotFoundException;
import com.bgsistemas.api.rate.mappers.RateQuestionMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-233: only the admin manages the questions of the wizard
@Import({SecurityConfig.class, RateQuestionMapper.class})
@WebMvcTest(AdminRateQuestionController.class)
class AdminRateQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateQuestionService rateQuestionService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private RateQuestion business() {
        RateQuestion question = new RateQuestion("Negocio", "¿Qué tipo de negocio tienes?", null, 0);
        question.setId(1L);
        RateOption restaurant = new RateOption("🍽️", "Restaurante", null, new BigDecimal("49.00"), AdjustmentType.BASE);
        restaurant.setId(1L);
        question.addOption(restaurant);
        return question;
    }

    @Test
    void shouldReturn401WithoutLogin() throws Exception {
        mockMvc.perform(get("/api/admin/rate-questions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListTheQuestionsWithTheirPrices() throws Exception {
        when(rateQuestionService.getAllQuestions()).thenReturn(List.of(business()));

        mockMvc.perform(get("/api/admin/rate-questions").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question").value("¿Qué tipo de negocio tienes?"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].options[0].amount").value(49.00))
                .andExpect(jsonPath("$[0].options[0].adjustmentType").value("BASE"));
    }

    @Test
    void shouldCreateAQuestion() throws Exception {
        RateQuestion saved = new RateQuestion("Carta", "¿Tienes carta digital?", null, 5);
        saved.setId(6L);
        when(rateQuestionService.createQuestion(any(RateQuestion.class))).thenReturn(saved);

        mockMvc.perform(post("/api/admin/rate-questions").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Carta\", \"question\": \"¿Tienes carta digital?\", \"active\": true }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.position").value(5));
    }

    @Test
    void shouldReturn400WhenTheQuestionHasNoText() throws Exception {
        mockMvc.perform(post("/api/admin/rate-questions").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Carta\", \"question\": \" \" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.question").value("Question is required"));

        verify(rateQuestionService, never()).createQuestion(any());
    }

    @Test
    void shouldReorderTheQuestions() throws Exception {
        when(rateQuestionService.reorderQuestions(List.of(2L, 1L))).thenReturn(List.of());

        mockMvc.perform(put("/api/admin/rate-questions/order").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"ids\": [2, 1] }"))
                .andExpect(status().isOk());

        verify(rateQuestionService).reorderQuestions(List.of(2L, 1L));
    }

    @Test
    void shouldAddAnOption() throws Exception {
        RateOption saved = new RateOption("💳", "Pago con tarjeta", null, new BigDecimal("5.00"), AdjustmentType.ADDITION);
        saved.setId(20L);
        when(rateQuestionService.addOption(eq(1L), any(RateOption.class))).thenReturn(saved);

        mockMvc.perform(post("/api/admin/rate-questions/1/options").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "icon": "💳", "label": "Pago con tarjeta", "amount": 5.00, "adjustmentType": "ADDITION" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.label").value("Pago con tarjeta"));
    }

    @Test
    void shouldReturn400WhenAFactorIsZero() throws Exception {
        when(rateQuestionService.updateOption(eq(7L), any(RateOption.class)))
                .thenThrow(new InvalidRateAmountException("A factor must be greater than 0"));

        mockMvc.perform(put("/api/admin/rate-questions/options/7").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"label\": \"2 o 3 locales\", \"amount\": 0, \"adjustmentType\": \"FACTOR\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A factor must be greater than 0"));
    }

    @Test
    void shouldReturn400WhenTheAmountIsNegative() throws Exception {
        mockMvc.perform(put("/api/admin/rate-questions/options/7").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"label\": \"Terraza\", \"amount\": -5, \"adjustmentType\": \"ADDITION\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").value("Amount cannot be negative"));
    }

    @Test
    void shouldDeleteAQuestionAndReturn404ForAMissingOption() throws Exception {
        mockMvc.perform(delete("/api/admin/rate-questions/1").with(user(admin)))
                .andExpect(status().isNoContent());
        verify(rateQuestionService).deleteQuestion(1L);

        org.mockito.Mockito.doThrow(new RateOptionNotFoundException(99L)).when(rateQuestionService).deleteOption(99L);
        mockMvc.perform(delete("/api/admin/rate-questions/options/99").with(user(admin)))
                .andExpect(status().isNotFound());
    }
}
