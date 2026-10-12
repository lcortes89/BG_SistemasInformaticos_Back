package com.bgsistemas.api.summary;

import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.security.SecurityUser;
import com.bgsistemas.api.summary.dtos.SummaryDTO;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(AdminSummaryController.class)
class AdminSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SummaryService summaryService;

    private final SecurityUser admin = new SecurityUser(new User("admin@mail.com", "hashed", Role.ADMIN));

    @Test
    void shouldReturnTheSummaryToTheAdmin() throws Exception {
        when(summaryService.getSummary()).thenReturn(new SummaryDTO(3, 12, 4, 5, 3, 10, 2, 3, List.of()));

        mockMvc.perform(get("/api/admin/summary").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newContacts").value(3))
                .andExpect(jsonPath("$.totalServices").value(10))
                .andExpect(jsonPath("$.latestContacts").isArray());
    }

    @Test
    void shouldReturn401WithoutSession() throws Exception {
        mockMvc.perform(get("/api/admin/summary"))
                .andExpect(status().isUnauthorized());

        verify(summaryService, never()).getSummary();
    }

    @Test
    void shouldReturn403WhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/summary").with(user("visitor@mail.com").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
