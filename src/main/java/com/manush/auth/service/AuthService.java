package com.manush.auth.service;

import com.manush.auth.dto.*;
import com.manush.auth.model.OtpCode;
import com.manush.auth.model.RefreshToken;
import com.manush.auth.model.User;
import com.manush.auth.repository.OtpCodeRepository;
import com.manush.auth.repository.RefreshTokenRepository;
import com.manush.auth.repository.UserRepository;
import com.manush.common.exception.ConflictException;
import com.manush.common.exception.ForbiddenException;
import com.manush.common.exception.ResourceNotFoundException;
import com.manush.common.exception.ValidationException;
import com.manush.common.model.Role;
import com.manush.common.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest req) {
        // Reject ADMIN registration
        if (req.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Cannot register as ADMIN");
        }
        // Require email or phone
        if ((req.getEmail() == null || req.getEmail().isBlank()) &&
            (req.getPhone() == null || req.getPhone().isBlank())) {
            throw new ValidationException("Email or phone is required");
        }
        // Duplicate checks
        if (req.getEmail() != null && !req.getEmail().isBlank() &&
            userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Email already registered");
        }
        if (req.getPhone() != null && !req.getPhone().isBlank() &&
            userRepository.existsByPhone(req.getPhone())) {
            throw new ConflictException("Phone already registered");
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .bloodGroup(req.getBloodGroup())
                .roles(Set.of(req.getRole() != null ? req.getRole() : Role.USER))
                .emailVerified(false)
                .phoneVerified(false)
                .profileComplete(false)
                .active(true)
                .banned(false)
                .helpingPoints(0)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {} [{}]", user.getId(), user.getEmail());

        // TODO: send email/OTP verification

        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        saveRefreshToken(user.getId(), refresh);

        return buildAuthResponse(user, access, refresh);
    }

    public AuthResponse login(LoginRequest req, String ipAddress, String userAgent) {
        String identifier = req.getIdentifier();
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive() || user.isBanned()) {
            throw new ForbiddenException("Account is suspended");
        }
        if (user.getPasswordHash() == null) {
            throw new BadCredentialsException("Please sign in with Google");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        saveRefreshToken(user.getId(), refresh, ipAddress, userAgent);

        return buildAuthResponse(user, access, refresh);
    }

    public AuthResponse refreshToken(RefreshRequest req) {
        if (!jwtService.isTokenValid(req.getRefreshToken())) {
            throw new ValidationException("Invalid or expired refresh token");
        }

        Claims claims = jwtService.extractAllClaims(req.getRefreshToken());
        if (!"refresh".equals(claims.get("type"))) {
            throw new ValidationException("Not a refresh token");
        }

        String userId = claims.getSubject();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String newAccess = jwtService.generateAccessToken(user);
        String newRefresh = jwtService.generateRefreshToken(user);
        saveRefreshToken(userId, newRefresh);

        return buildAuthResponse(user, newAccess, newRefresh);
    }

    public void logout(String userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    public void forgotPassword(String identifier) {
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElse(null);
        
        if (user == null) {
            log.info("Password reset requested for non-existent identifier: {}", identifier);
            return;
        }
        
        log.info("Password reset requested for user: {} [{}]", user.getId(), identifier);
        // TODO: Send password reset email/SMS with reset token
    }

    private void saveRefreshToken(String userId, String rawToken) {
        saveRefreshToken(userId, rawToken, null, null);
    }

    private void saveRefreshToken(String userId, String rawToken, String ip, String ua) {
        RefreshToken rt = RefreshToken.builder()
                .userId(userId)
                .tokenHash(passwordEncoder.encode(rawToken))
                .ipAddress(ip)
                .deviceInfo(ua)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiry()))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(rt);
    }

    private AuthResponse buildAuthResponse(User user, String access, String refresh) {
        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileComplete(user.isProfileComplete())
                .roles(user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()))
                .build();
    }
}
