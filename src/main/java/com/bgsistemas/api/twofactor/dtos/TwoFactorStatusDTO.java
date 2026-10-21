package com.bgsistemas.api.twofactor.dtos;

// BGSI-276: GET /api/admin/account/2fa
public record TwoFactorStatusDTO(boolean enabled, long recoveryCodesLeft) {
}
