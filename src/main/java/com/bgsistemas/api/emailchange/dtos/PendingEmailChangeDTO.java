package com.bgsistemas.api.emailchange.dtos;

import java.time.Instant;

// BGSI-299: the change waiting for the confirmation link. Both null = nothing pending
public record PendingEmailChangeDTO(String newEmail, Instant expiresAt) {

    public static PendingEmailChangeDTO none() {
        return new PendingEmailChangeDTO(null, null);
    }
}
