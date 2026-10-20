package com.bgsistemas.api.account;

import com.bgsistemas.api.account.dtos.ChangePasswordRequestDTO;
import com.bgsistemas.api.account.dtos.ChangePasswordResponseDTO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// BGSI-272: "Mi cuenta" of the admin panel (/api/admin/** -> only ADMIN, see SecurityConfig)
@RestController
@RequestMapping("/api/admin/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // PUT /api/admin/account/password -> 200 { "token": "..." }
    // authentication.getName() = email of the logged-in admin (the "sub" of the JWT)
    @PutMapping("/password")
    public ChangePasswordResponseDTO changePassword(@Valid @RequestBody ChangePasswordRequestDTO request,
                                                   Authentication authentication) {
        String token = accountService.changePassword(authentication.getName(),
                request.currentPassword(), request.newPassword());
        return new ChangePasswordResponseDTO(token);
    }
}
