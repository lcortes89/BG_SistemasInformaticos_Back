package com.bgsistemas.api.twofactor;

import com.bgsistemas.api.security.RequiresConfirmation;
import com.bgsistemas.api.twofactor.dtos.RecoveryCodesDTO;
import com.bgsistemas.api.twofactor.dtos.TwoFactorCodeRequestDTO;
import com.bgsistemas.api.twofactor.dtos.TwoFactorSetupDTO;
import com.bgsistemas.api.twofactor.dtos.TwoFactorStatusDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// BGSI-276: "Mi cuenta", two-step verification (/api/admin/** -> only ADMIN)
@RestController
@RequestMapping("/api/admin/account/2fa")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    public TwoFactorController(TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
    }

    // GET -> { enabled, recoveryCodesLeft }
    @GetMapping
    public TwoFactorStatusDTO status(Authentication authentication) {
        TwoFactorService.Status status = twoFactorService.status(authentication.getName());
        return new TwoFactorStatusDTO(status.enabled(), status.recoveryCodesLeft());
    }

    // POST /setup -> { secret, otpauthUri } (409 if it is already on)
    @PostMapping("/setup")
    public TwoFactorSetupDTO setup(Authentication authentication) {
        TwoFactorService.Setup setup = twoFactorService.setup(authentication.getName());
        return new TwoFactorSetupDTO(setup.secret(), setup.otpauthUri());
    }

    // POST /enable { code } -> { recoveryCodes } (400 wrong code)
    @PostMapping("/enable")
    public RecoveryCodesDTO enable(@Valid @RequestBody TwoFactorCodeRequestDTO request, Authentication authentication) {
        return new RecoveryCodesDTO(twoFactorService.enable(authentication.getName(), request.code()));
    }

    // DELETE -> 204. Needs the password and the code (BGSI-275 confirmation)
    @RequiresConfirmation
    @DeleteMapping
    public ResponseEntity<Void> disable(Authentication authentication) {
        twoFactorService.disable(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
