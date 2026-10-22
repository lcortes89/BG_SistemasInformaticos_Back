package com.bgsistemas.api.emailchange;

import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.emailchange.dtos.PendingEmailChangeDTO;
import com.bgsistemas.api.emailchange.exceptions.InvalidEmailChangeTokenException;
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

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-299
@Import(SecurityConfig.class)
@WebMvcTest(EmailChangeController.class)
class EmailChangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailChangeService emailChangeService;

    private final SecurityUser admin = new SecurityUser(new User("luisa@bg.es", "hashed", Role.ADMIN));

    @Test
    void asksForTheChange() throws Exception {
        when(emailChangeService.request("luisa@bg.es", "nueva@bg.es"))
                .thenReturn(new PendingEmailChangeDTO("nueva@bg.es", Instant.parse("2026-10-22T11:00:00Z")));

        mockMvc.perform(post("/api/admin/account/email").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"newEmail\": \"nueva@bg.es\" }"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.newEmail").value("nueva@bg.es"));
    }

    @Test
    void rejectsAWrongEmailAndOneThatExists() throws Exception {
        mockMvc.perform(post("/api/admin/account/email").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"newEmail\": \"no-email\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.newEmail").value("Email format is not valid"));

        when(emailChangeService.request("luisa@bg.es", "maria@bg.es")).thenThrow(new EmailAlreadyExistsException("maria@bg.es"));
        mockMvc.perform(post("/api/admin/account/email").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"newEmail\": \"maria@bg.es\" }"))
                .andExpect(status().isConflict());
    }

    @Test
    void showsAndCancelsThePendingChange() throws Exception {
        when(emailChangeService.pending("luisa@bg.es")).thenReturn(PendingEmailChangeDTO.none());

        mockMvc.perform(get("/api/admin/account/email").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newEmail").doesNotExist());
        mockMvc.perform(delete("/api/admin/account/email").with(user(admin))).andExpect(status().isNoContent());

        verify(emailChangeService).cancel("luisa@bg.es");
    }

    @Test
    void theLinkWorksWithoutASession() throws Exception {
        when(emailChangeService.confirm("tok")).thenReturn("nueva@bg.es");

        mockMvc.perform(post("/api/auth/confirm-email").contentType(MediaType.APPLICATION_JSON).content("{ \"token\": \"tok\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nueva@bg.es"));
    }

    @Test
    void aBadLinkIs400() throws Exception {
        when(emailChangeService.confirm("old")).thenThrow(new InvalidEmailChangeTokenException());

        mockMvc.perform(post("/api/auth/confirm-email").contentType(MediaType.APPLICATION_JSON).content("{ \"token\": \"old\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void askingForTheChangeNeedsASession() throws Exception {
        mockMvc.perform(get("/api/admin/account/email")).andExpect(status().isUnauthorized());
    }
}
