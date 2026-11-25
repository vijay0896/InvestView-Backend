package com.investview.auth;

import java.util.UUID;

/**
 * Response payload returned after successful registration or login.
 */
public record AuthResponse(
        String token,
        UUID userId,
        String fullName,
        String email
) {
}