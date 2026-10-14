package com.bgsistemas.api.setting;

import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Whole application + H2 database: the admin changes the photo and the public endpoint sees it
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class HeroImageIntegrationTest {

    private static final String URL = "http://localhost:8080/uploads/new-hero.jpg";

    @Autowired
    private MockMvc mockMvc;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    @Test
    void adminChangesThePhotoAndTheWebsiteSeesItUntilItIsReset() throws Exception {
        mockMvc.perform(get("/api/settings/hero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());

        mockMvc.perform(put("/api/admin/settings/hero").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"imageUrl\": \"" + URL + "\" }"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/settings/hero"))
                .andExpect(jsonPath("$.imageUrl").value(URL));

        mockMvc.perform(delete("/api/admin/settings/hero").with(user(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/settings/hero"))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
    }
}
