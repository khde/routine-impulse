package org.routineimpulse.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.core.SecurityContext;
import io.quarkus.logging.Log;

import org.routineimpulse.dto.LoginRequest;
import org.routineimpulse.dto.LoginResponse;
import org.routineimpulse.model.RefreshToken;
import org.routineimpulse.dto.SignupRequest;
import org.routineimpulse.model.User;

@ApplicationScoped
public class AuthService {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    UserService userService;

    @Inject
    SecurityContext securityContext;

    @Inject
    EntityManager em;

    @Transactional
    public LoginResponse register(SignupRequest request) {
        String normalizedUsername = request.getUsername().toLowerCase().trim();
        Log.infof("New Registration: %s", normalizedUsername);

        if (userService.getUserByUsername(normalizedUsername) != null) {
            Log.warnf("Registration failed: username already exists: %s", normalizedUsername);
            throw new BadRequestException("Username already in use");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(request.getEmail());
        user.setLocked(false);

        String encryptedPassword = BcryptUtil.bcryptHash(request.getPassword());
        user.setPassword(encryptedPassword);

        userService.createUser(user);
        Log.infof("New user registered: %s", normalizedUsername);

        LoginResponse response = new LoginResponse();
        response.setUsername(user.getUsername());

        return response;
    }

    public LoginResponse authenticate(LoginRequest request) {
        String normalizedUsername = request.getUsername().toLowerCase().trim();
        User user = userService.getUserByUsername(normalizedUsername);

        if (user == null || !BcryptUtil.matches(request.getPassword(), user.getPassword())) {
            Log.warnf("Authentication failed: invalid credentials for user: %s", normalizedUsername);
            throw new NotAuthorizedException("Invalid credentials");
        }

        if (user.isLocked()) {
            Log.warnf("Authentication failed: user account is locked: %s", normalizedUsername);
            throw new NotAuthorizedException("Account is locked");
        }

        String username = user.getUsername();
        Log.infof("User authenticated: %s", username);

        LoginResponse response = new LoginResponse();
        response.setUsername(username);

        return response;
    }

    @Transactional
    public String createRefreshToken(String username) {
        User user = userService.getUserByUsername(username);
        if (user == null || user.isLocked()) {
            throw new NotAuthorizedException("Invalid credentials");
        }

        em.createQuery("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
            .setParameter("userId", user.getId())
            .executeUpdate();

        String refreshTokenValue = generateRefreshTokenValue();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(hashRefreshToken(refreshTokenValue));
        refreshToken.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        refreshToken.setRevoked(false);
        em.persist(refreshToken);

        return refreshTokenValue;
    }

    @Transactional
    public String createAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new NotAuthorizedException("Invalid refresh token");
        }

        String refreshTokenHash = hashRefreshToken(refreshToken);

        Optional<RefreshToken> storedTokenOptional = em.createNamedQuery("RefreshToken.findByToken", RefreshToken.class)
            .setParameter("token", refreshTokenHash)
            .getResultStream()
            .findFirst();

        if (storedTokenOptional.isEmpty()) {
            Log.warn("Refresh failed: refresh token is invalid");
            throw new NotAuthorizedException("Invalid refresh token");
        }

        RefreshToken storedToken = storedTokenOptional.get();
        if (storedToken.isRevoked() || storedToken.isExpired()) {
            Log.warn("Refresh failed: refresh token is invalid, revoked or expired");
            throw new NotAuthorizedException("Invalid refresh token");
        }

        User user = em.find(User.class, storedToken.getUserId());
        if (user == null || user.isLocked()) {
            Log.warn("Refresh failed: user not found or locked");
            storedToken.setRevoked(true);
            throw new NotAuthorizedException("Invalid refresh token");
        }

        return Jwt.issuer("routineimpulse")
            .upn(user.getUsername())
            .claim("token_type", ACCESS_TOKEN_TYPE)
            .expiresIn(Duration.ofMinutes(15))
            .sign();
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String refreshTokenHash = hashRefreshToken(refreshToken);

        em.createNamedQuery("RefreshToken.findByToken", RefreshToken.class)
            .setParameter("token", refreshTokenHash)
            .getResultStream()
            .findFirst()
            .ifPresent(token -> {
                token.setRevoked(true);
                Log.infof("Refresh token revoked during logout for userId=%s", token.getUserId());
            });
    }

    private String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Refresh token hashing algorithm is not available", e);
        }
    }

    public String getCurrentUsername() {
        if (securityContext.getUserPrincipal() == null) {
            Log.warn("Authentication required but not provided");
            throw new NotAuthorizedException("Authentication required");
        }
        
        String username = securityContext.getUserPrincipal().getName();
        User user = userService.getUserByUsername(username);
        
        if (user != null && user.isLocked()) {
            Log.warnf("Access denied: user account is locked: %s", username);
            throw new NotAuthorizedException("Account is locked");
        }
        
        return username;
    }
}