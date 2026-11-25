package com.investview.auth;

import com.investview.common.exception.BusinessException;
import com.investview.common.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementation of AuthService handling registration, BCrypt verification, and JWT issuance.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserCredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           UserCredentialsRepository credentialsRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.credentialsRepository = credentialsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email is already registered", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber().trim())) {
            throw new BusinessException("Phone number is already registered", HttpStatus.CONFLICT);
        }

        // 1. Save User Profile (generates UUIDv7)
        User user = new User(
                request.fullName().trim(),
                normalizedEmail,
                request.phoneNumber().trim(),
                request.dateOfBirth()
        );
        userRepository.save(user);

        // 2. Hash Password with BCrypt & Save isolated UserCredentials
        String passwordHash = passwordEncoder.encode(request.password());
        UserCredentials credentials = new UserCredentials(user.getId(), passwordHash);
        credentialsRepository.save(credentials);

        // 3. Issue JWT Token
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!user.getIsActive()) {
            throw new BusinessException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        UserCredentials credentials = credentialsRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        // Verify raw password against BCrypt hash
        if (!passwordEncoder.matches(request.password(), credentials.getPasswordHash())) {
            credentials.setFailedAttempts(credentials.getFailedAttempts() + 1);
            credentialsRepository.save(credentials);
            throw new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        // Reset failed attempts & update last login timestamp
        credentials.setFailedAttempts(0);
        credentials.setLastLoginAt(Instant.now());
        credentialsRepository.save(credentials);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail());
    }
}