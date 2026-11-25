package com.investview.auth;

/**
 * Service contract for user registration and authentication.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}