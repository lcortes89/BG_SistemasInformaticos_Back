package com.bgsistemas.api.setting;

import com.bgsistemas.api.config.SecurityConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(AdminSettingController.class)
class AdminSettingControllerTest {

    private static final String URL = "http://localhost:8080/uploads/hero.jpg";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SiteSettingService siteSettingService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    @Test
    void shouldUpdateTheHeroImage() throws Exception {
        when(siteSettingService.updateHeroImageUrl(URL)).thenReturn(URL);

        mockMvc.perform(put("/api/admin/settings/hero").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"imageUrl\": \"" + URL + "\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(URL));
    }

    @Test
    void shouldReturn400WhenTheUrlIsEmpty() throws Exception {
        mockMvc.perform(put("/api/admin/settings/hero").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"imageUrl\": \"\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.imageUrl").exists());

        verify(siteSettingService, never()).updateHeroImageUrl(any());
    }

    @Test
    void shouldReturn400WhenTheUrlIsNotHttp() throws Exception {
        mockMvc.perform(put("/api/admin/settings/hero").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"imageUrl\": \"javascript:alert(1)\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.imageUrl").value("Image URL must start with http:// or https://"));
    }

    @Test
    void shouldResetTheHeroImage() throws Exception {
        mockMvc.perform(delete("/api/admin/settings/hero").with(user(admin)))
                .andExpect(status().isNoContent());

        verify(siteSettingService).resetHeroImage();
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(put("/api/admin/settings/hero")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"imageUrl\": \"" + URL + "\" }"))
                .andExpect(status().isUnauthorized());

        verify(siteSettingService, never()).updateHeroImageUrl(any());
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/settings/hero").with(user("visitor@mail.com").roles("USER")))
                .andExpect(status().isForbidden());

        verify(siteSettingService, never()).resetHeroImage();
    }
}
