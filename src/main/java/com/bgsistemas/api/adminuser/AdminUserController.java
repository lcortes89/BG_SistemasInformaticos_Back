package com.bgsistemas.api.adminuser;

import com.bgsistemas.api.adminuser.dtos.AdminEmailRequestDTO;
import com.bgsistemas.api.adminuser.dtos.AdminUserDTO;
import com.bgsistemas.api.security.RequiresConfirmation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// BGSI-277: "Administradores" (/api/admin/** -> only ADMIN). The changes need the password (BGSI-275)
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserDTO> list(Authentication authentication) {
        return adminUserService.list(authentication.getName());
    }

    // POST /invitations { email } -> 201 (409 if the email already exists)
    @RequiresConfirmation
    @PostMapping("/invitations")
    public ResponseEntity<AdminUserDTO> invite(@Valid @RequestBody AdminEmailRequestDTO request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.invite(request.email(), authentication.getName()));
    }

    // POST /{id}/invitation -> 204, a new link for a pending admin
    @PostMapping("/{id}/invitation")
    public ResponseEntity<Void> resendInvitation(@PathVariable Long id, Authentication authentication) {
        adminUserService.resendInvitation(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @RequiresConfirmation
    @PutMapping("/{id}/block")
    public AdminUserDTO block(@PathVariable Long id, Authentication authentication) {
        return adminUserService.block(id, authentication.getName());
    }

    @RequiresConfirmation
    @PutMapping("/{id}/unblock")
    public AdminUserDTO unblock(@PathVariable Long id, Authentication authentication) {
        return adminUserService.unblock(id, authentication.getName());
    }

    @RequiresConfirmation
    @DeleteMapping("/{id}/2fa")
    public AdminUserDTO resetTwoFactor(@PathVariable Long id, Authentication authentication) {
        return adminUserService.resetTwoFactor(id, authentication.getName());
    }

    @RequiresConfirmation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        adminUserService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
