package com.bgsistemas.api.adminuser.dtos;

import java.time.Instant;

// BGSI-277: one row of "Administradores". you = the logged-in admin (no actions on yourself here)
public record AdminUserDTO(Long id, String email, String status, boolean twoFactorEnabled, Instant lastLoginAt, boolean you) {
}
