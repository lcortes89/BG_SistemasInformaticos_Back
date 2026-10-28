package com.bgsistemas.api.setting;

import com.bgsistemas.api.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(SettingController.class)
class SettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteSettingService siteSettingService;

    @Test
    void shouldReturnTheHeroImageWithoutLoggingIn() throws Exception {
        when(siteSettingService.getHeroImageUrl()).thenReturn(Optional.of("http://localhost:8080/uploads/hero.jpg"));

        mockMvc.perform(get("/api/settings/hero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("http://localhost:8080/uploads/hero.jpg"));
    }

    @Test
    void shouldReturnNullImageWhenNoneWasChosen() throws Exception {
        when(siteSettingService.getHeroImageUrl()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/settings/hero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
    }
}
