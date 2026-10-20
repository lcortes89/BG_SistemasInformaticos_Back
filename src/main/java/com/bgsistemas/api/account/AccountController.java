package com.bgsistemas.api.account;

import com.bgsistemas.api.account.dtos.ChangePasswordRequestDTO;
import com.bgsistemas.api.account.dtos.ChangePasswordResponseDTO;
import com.bgsistemas.api.account.dtos.ConfirmPasswordRequestDTO;
import com.bgsistemas.api.account.dtos.ConfirmPasswordResponseDTO;
import com.bgsistemas.api.security.ConfirmationTokenService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// BGSI-272: "Mi cuenta" of the admin panel (/api/admin/** -> only ADMIN, see SecurityConfig)
@RestController
@RequestMapping("/api/admin/account")
public class AccountController {

    private final AccountService accountService;
    private final PasswordConfirmationService passwordConfirmationService;

    public AccountController(AccountService accountService, PasswordConfirmationService passwordConfirmationService) {
        this.accountService = accountService;
        this.passwordConfirmationService = passwordConfirmationService;
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

    // BGSI-275: POST /api/admin/account/confirm { password } -> 200 { confirmToken, expiresInSeconds }
    // 400 wrong password, 429 after 5 wrong passwords (same lock as the login)
    @PostMapping("/confirm")
    public ConfirmPasswordResponseDTO confirmPassword(@Valid @RequestBody ConfirmPasswordRequestDTO request,
                                                     Authentication authentication) {
        String token = passwordConfirmationService.confirm(authentication.getName(), request.password());
        return new ConfirmPasswordResponseDTO(token, ConfirmationTokenService.VALIDITY.toSeconds());
    }
}
