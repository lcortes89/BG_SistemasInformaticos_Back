package com.bgsistemas.api.twofactor.dtos;

// BGSI-276: the frontend draws the QR from otpauthUri; the secret is shown to type it by hand
public record TwoFactorSetupDTO(String secret, String otpauthUri) {
}
