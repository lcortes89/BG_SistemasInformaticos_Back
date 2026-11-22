package com.bgsistemas.api.twofactor.dtos;

// BGSI-276: GET /api/admin/account/2fa
// BGSI-337: entryName = name of the entry in the app (null when 2FA is off)
public record TwoFactorStatusDTO(boolean enabled, long recoveryCodesLeft, String entryName) {
}
