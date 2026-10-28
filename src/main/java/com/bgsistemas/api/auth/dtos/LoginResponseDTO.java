package com.bgsistemas.api.auth.dtos;

// Normal login: { token, email, role } (twoFactorRequired and pendingToken are null).
// BGSI-276 with 2FA on: { email, twoFactorRequired: true, pendingToken } and the code is asked next
public record LoginResponseDTO(String token, String email, String role, Boolean twoFactorRequired, String pendingToken) {

    public LoginResponseDTO(String token, String email, String role) {
        this(token, email, role, null, null);
    }

    public static LoginResponseDTO twoFactorPending(String email, String pendingToken) {
        return new LoginResponseDTO(null, email, null, true, pendingToken);
    }

    public boolean needsSecondStep() {
        return Boolean.TRUE.equals(twoFactorRequired);
    }
}
