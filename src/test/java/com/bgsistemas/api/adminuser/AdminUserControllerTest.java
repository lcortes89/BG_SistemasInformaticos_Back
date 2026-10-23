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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
    void listsOnePageOfAdmins() throws Exception {
        when(adminUserService.list(eq("luisa@bg.es"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(maria), PageRequest.of(0, 5), 6));

        mockMvc.perform(get("/api/admin/users").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("maria@bg.es"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].twoFactorEnabled").value(true))
                .andExpect(jsonPath("$.content[0].you").value(false))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    // BGSI-315: 5 per page by default, sorted by email
    @Test
    void asksForFiveAdminsByEmailByDefault() throws Exception {
        when(adminUserService.list(eq("luisa@bg.es"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/users").with(user(admin))).andExpect(status().isOk());

        verify(adminUserService).list(eq("luisa@bg.es"), argThat(pageable -> pageable.getPageNumber() == 0
                && pageable.getPageSize() == 5 && pageable.getSort().equals(Sort.by("email"))));
    }

    @Test
    void neverAsksForMoreThan50AdminsOrANegativePage() throws Exception {
        when(adminUserService.list(eq("luisa@bg.es"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/users").param("page", "-3").param("size", "500").with(user(admin)))
                .andExpect(status().isOk());

        verify(adminUserService).list(eq("luisa@bg.es"),
                argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == 50));
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
    void blocksUnblocksResetsTwoFactorAndDeletes() throws Exception {
        when(adminUserService.block(2L, "luisa@bg.es")).thenReturn(maria);
        when(adminUserService.unblock(2L, "luisa@bg.es")).thenReturn(maria);
        when(adminUserService.resetTwoFactor(2L, "luisa@bg.es")).thenReturn(maria);

        mockMvc.perform(put("/api/admin/users/2/block").with(user(admin))).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/users/2/unblock").with(user(admin))).andExpect(status().isOk());
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
