package com.rentacar.dto.auth;

public record LoginResponse(
        String token,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String role
) {
}
