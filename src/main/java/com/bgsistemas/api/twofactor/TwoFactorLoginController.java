package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.auth.dtos.LoginResponseDTO;
import com.bgsistemas.api.twofactor.dtos.TwoFactorLoginRequestDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// BGSI-276: POST /api/auth/login/2fa { pendingToken, code } -> 200 { token, email, role }
// 400 wrong code or expired login, 429 after 5 wrong codes (same lock as the login)
@RestController
public class TwoFactorLoginController {

    private final TwoFactorLoginService twoFactorLoginService;

    public TwoFactorLoginController(TwoFactorLoginService twoFactorLoginService) {
        this.twoFactorLoginService = twoFactorLoginService;
    }

    @PostMapping("/api/auth/login/2fa")
    public LoginResponseDTO verify(@Valid @RequestBody TwoFactorLoginRequestDTO request) {
        return twoFactorLoginService.verify(request.pendingToken(), request.code());
    }
}
