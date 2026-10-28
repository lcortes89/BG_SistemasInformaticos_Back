package com.bgsistemas.api.account.dtos;

// BGSI-275: the frontend sends confirmToken in the X-Confirm-Token header during expiresInSeconds
public record ConfirmPasswordResponseDTO(String confirmToken, long expiresInSeconds) {
}
