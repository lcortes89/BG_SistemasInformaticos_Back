package com.bgsistemas.api.rate;

import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-233: whole application + H2. The initializer creates the 5 questions, the website
// reads them, calculates a price, and the admin changes a price and hides a question
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class RateWizardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    private String calculateBody(List<Integer> optionIds) {
        return "{ \"optionIds\": " + optionIds + " }";
    }

    @Test
    void theWizardWorksFromTheDatabaseAndTheAdminCanChangeIt() throws Exception {
        String steps = mockMvc.perform(get("/api/rates/steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].title").value("Negocio"))
                .andReturn().getResponse().getContentAsString();

        // First option of every question: Restaurante, 1 local, 1 TPV, interior, < 100 m² -> 49 €
        List<Integer> firstOptions = JsonPath.read(steps, "$[*].options[0].id");
        mockMvc.perform(post("/api/rates/calculate")
                        .contentType(MediaType.APPLICATION_JSON).content(calculateBody(firstOptions)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPrice").value(49.00));

        // Without the last answer -> 400
        mockMvc.perform(post("/api/rates/calculate")
                        .contentType(MediaType.APPLICATION_JSON).content(calculateBody(firstOptions.subList(0, 4))))
                .andExpect(status().isBadRequest());

        // The admin raises "Restaurante" to 55 €
        Integer restaurantId = firstOptions.get(0);
        mockMvc.perform(put("/api/admin/rate-questions/options/" + restaurantId).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"icon\": \"🍽️\", \"label\": \"Restaurante\", \"amount\": 55.00, \"adjustmentType\": \"BASE\" }"))
                .andExpect(status().isOk());

        // The admin hides "Superficie": the wizard now has 4 steps and 4 answers are enough
        String questions = mockMvc.perform(get("/api/admin/rate-questions").with(user(admin)))
                .andReturn().getResponse().getContentAsString();
        Integer surfaceId = JsonPath.read(questions, "$[4].id");
        mockMvc.perform(put("/api/admin/rate-questions/" + surfaceId).with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Superficie\", \"question\": \"¿Cuál es la superficie aproximada?\", \"active\": false }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/rates/steps"))
                .andExpect(jsonPath("$.length()").value(4));
        mockMvc.perform(post("/api/rates/calculate")
                        .contentType(MediaType.APPLICATION_JSON).content(calculateBody(firstOptions.subList(0, 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPrice").value(55.00));
    }
}
