package com.bgsistemas.api.auth.dtos;

public record LoginResponseDTO(String token, String email, String role) {
}
