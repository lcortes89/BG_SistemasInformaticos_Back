package com.bgsistemas.api.emailchange.dtos;

// BGSI-299: the new email, so the page can say "log in with ..."
public record ConfirmEmailChangeResponseDTO(String email) {
}
