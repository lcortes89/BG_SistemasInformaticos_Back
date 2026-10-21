package com.bgsistemas.api.adminuser;

import com.bgsistemas.api.adminuser.dtos.AdminUserDTO;
import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import com.bgsistemas.api.config.SecurityConfig;
import com.bgsistemas.api.globals.ResourceConflictException;
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
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// BGSI-277: /api/admin/users
@Import(SecurityConfig.class)
@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    private final SecurityUser admin = new SecurityUser(new User("luisa@bg.es", "hashed", Role.ADMIN));
    private final AdminUserDTO maria = new AdminUserDTO(2L, "maria@bg.es", "ACTIVE", true, Instant.parse("2026-10-21T10:00:00Z"), false);

    @Test
    void listsTheAdmins() throws Exception {
        when(adminUserService.list("luisa@bg.es")).thenReturn(List.of(maria));

        mockMvc.perform(get("/api/admin/users").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("maria@bg.es"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].twoFactorEnabled").value(true))
                .andExpect(jsonPath("$[0].you").value(false));
    }

    @Test
    void invitesAnAdmin() throws Exception {
        when(adminUserService.invite("nuevo@bg.es", "luisa@bg.es"))
                .thenReturn(new AdminUserDTO(3L, "nuevo@bg.es", "PENDING", false, null, false));

        mockMvc.perform(post("/api/admin/users/invitations").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"nuevo@bg.es\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsAWrongEmail() throws Exception {
        mockMvc.perform(post("/api/admin/users/invitations").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"not-an-email\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").value("Email format is not valid"));
    }

    @Test
    void anEmailThatExistsIs409() throws Exception {
        when(adminUserService.invite("maria@bg.es", "luisa@bg.es")).thenThrow(new EmailAlreadyExistsException("maria@bg.es"));

        mockMvc.perform(post("/api/admin/users/invitations").with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"maria@bg.es\" }"))
                .andExpect(status().isConflict());
    }

    @Test
    void blocksUnblocksChangesTheEmailResetsTwoFactorAndDeletes() throws Exception {
        when(adminUserService.block(2L, "luisa@bg.es")).thenReturn(maria);
        when(adminUserService.unblock(2L, "luisa@bg.es")).thenReturn(maria);
        when(adminUserService.changeEmail(2L, "maria2@bg.es", "luisa@bg.es")).thenReturn(maria);
        when(adminUserService.resetTwoFactor(2L, "luisa@bg.es")).thenReturn(maria);

        mockMvc.perform(put("/api/admin/users/2/block").with(user(admin))).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/users/2/unblock").with(user(admin))).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/users/2/email").with(user(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"maria2@bg.es\" }")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/users/2/2fa").with(user(admin))).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/users/2/invitation").with(user(admin))).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/users/2").with(user(admin))).andExpect(status().isNoContent());

        verify(adminUserService).resendInvitation(2L, "luisa@bg.es");
        verify(adminUserService).delete(2L, "luisa@bg.es");
    }

    @Test
    void actingOnYourselfIs409() throws Exception {
        when(adminUserService.block(1L, "luisa@bg.es")).thenThrow(new ResourceConflictException("You cannot do this to your own account"));

        mockMvc.perform(put("/api/admin/users/1/block").with(user(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    void needsToBeLoggedIn() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }
}
