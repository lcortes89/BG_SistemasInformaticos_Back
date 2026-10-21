package com.bgsistemas.api.twofactor.dtos;

import jakarta.validation.constraints.NotBlank;

// BGSI-276: body of POST /api/admin/account/2fa/enable
public record TwoFactorCodeRequestDTO(

        @NotBlank(message = "Code is required")
        String code
) {
}
