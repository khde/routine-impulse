package org.routineimpulse.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.core.SecurityContext;
import io.quarkus.logging.Log;

import org.routineimpulse.dto.LoginRequest;
import org.routineimpulse.dto.LoginResponse;
import org.routineimpulse.dto.SessionResponse;
import org.routineimpulse.dto.SessionTokens;
import org.routineimpulse.exception.AuthException;
import org.routineimpulse.model.SessionCredential;
import org.routineimpulse.dto.ChangePasswordRequest;
import org.routineimpulse.dto.SignupRequest;
import org.routineimpulse.model.User;
import org.routineimpulse.model.UserSession;

@ApplicationScoped
public class AuthService {

    private static final String DEFAULT_USER_ROLE = "user";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    UserService userService;

    @Inject
    SecurityContext securityContext;

    @Inject
    EntityManager em;

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String jwtIssuer;

    @Transactional
    public LoginResponse register(SignupRequest request) {
        String normalizedUsername = request.getUsername().toLowerCase().trim();
        Log.infof("New Registration: %s", normalizedUsername);

        if (userService.getUserByUsername(normalizedUsername) != null) {
            Log.warnf("Registration failed: username already exists: %s", normalizedUsername);
            throw new AuthException(Response.Status.BAD_REQUEST, "USERNAME_ALREADY_IN_USE", "Username is already in use");
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
            throw new AuthException("INVALID_CREDENTIALS", "Invalid credentials");
        }

        if (user.isLocked()) {
            Log.warnf("Authentication failed: user account is locked: %s", normalizedUsername);
            throw new AuthException("ACCOUNT_LOCKED", "Account is locked");
        }

        String username = user.getUsername();
        Log.infof("User authenticated: %s", username);

        LoginResponse response = new LoginResponse();
        response.setUsername(username);

        return response;
    }

    @Transactional
    public SessionTokens createSession(String username, String userAgent) {
        User user = userService.getUserByUsername(username);
        if (user == null || user.isLocked()) {
            throw new AuthException("INVALID_CREDENTIALS", "Invalid credentials");
        }

        Instant now = Instant.now();
        em.createQuery("DELETE FROM SessionCredential c WHERE c.sessionId IN " +
                "(SELECT s.id FROM UserSession s WHERE s.userId = :userId AND (s.revoked = true OR s.expiresAt <= :now))")
            .setParameter("userId", user.getId())
            .setParameter("now", now)
            .executeUpdate();
        em.createQuery("DELETE FROM UserSession s WHERE s.userId = :userId AND (s.revoked = true OR s.expiresAt <= :now)")
            .setParameter("userId", user.getId())
            .setParameter("now", now)
            .executeUpdate();

        String sessionToken = generateSessionTokenValue();
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setUserAgent(limitUserAgent(userAgent));
        session.setExpiresAt(now.plus(Duration.ofDays(7)));
        em.persist(session);
        em.flush();

        SessionCredential credential = new SessionCredential();
        credential.setSessionId(session.getId());
        credential.setTokenHash(hashSessionToken(sessionToken));
        credential.setCreatedAt(now);
        em.persist(credential);

        return createSessionTokens(user, session, sessionToken);
    }

    @Transactional(dontRollbackOn = AuthException.class)
    public SessionTokens rotateSessionToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw invalidSessionToken();
        }

        Optional<SessionCredential> credentialOptional = em
            .createNamedQuery("SessionCredential.findByTokenHash", SessionCredential.class)
            .setParameter("tokenHash", hashSessionToken(sessionToken))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultStream()
            .findFirst();

        if (credentialOptional.isEmpty()) {
            Log.warn("Session refresh failed: credential is invalid");
            throw invalidSessionToken();
        }

        SessionCredential credential = credentialOptional.get();
        UserSession session = em.find(UserSession.class, credential.getSessionId(), LockModeType.PESSIMISTIC_WRITE);
        if (credential.getConsumedAt() != null) {
            if (session != null) {
                session.setRevoked(true);
            }
            Log.warn("Session refresh failed: previously rotated credential was reused");
            throw invalidSessionToken();
        }

        if (session == null || session.isRevoked() || !session.getExpiresAt().isAfter(Instant.now())) {
            Log.warn("Session refresh failed: session is revoked or expired");
            throw invalidSessionToken();
        }

        User user = em.find(User.class, session.getUserId());
        if (user == null || user.isLocked()) {
            session.setRevoked(true);
            throw invalidSessionToken();
        }

        Instant now = Instant.now();
        credential.setConsumedAt(now);
        session.setLastUsedAt(now);
        String nextSessionToken = generateSessionTokenValue();
        SessionCredential nextCredential = new SessionCredential();
        nextCredential.setSessionId(session.getId());
        nextCredential.setTokenHash(hashSessionToken(nextSessionToken));
        nextCredential.setCreatedAt(now);
        em.persist(nextCredential);

        return createSessionTokens(user, session, nextSessionToken);
    }

    @Transactional
    public void logoutSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }

        em.createNamedQuery("SessionCredential.findByTokenHash", SessionCredential.class)
            .setParameter("tokenHash", hashSessionToken(sessionToken))
            .getResultStream()
            .findFirst()
            .ifPresent(credential -> {
                UserSession session = em.find(UserSession.class, credential.getSessionId());
                if (session != null) {
                    session.setRevoked(true);
                    Log.infof("Session revoked during logout for sessionId=%s", session.getId());
                }
            });
    }

    public List<SessionResponse> getSessions(String username, String currentSessionToken) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new AuthException("AUTHENTICATION_REQUIRED", "Authentication required");
        }

        Long currentSessionId = findSessionIdForToken(currentSessionToken);
        Instant now = Instant.now();
        return em.createQuery(
                "SELECT s FROM UserSession s WHERE s.userId = :userId AND s.revoked = false " +
                    "AND s.expiresAt > :now ORDER BY s.createdAt DESC", UserSession.class)
            .setParameter("userId", user.getId())
            .setParameter("now", now)
            .getResultList()
            .stream()
            .map(session -> toSessionResponse(session, session.getId().equals(currentSessionId)))
            .toList();
    }

    @Transactional
    public Boolean revokeSession(String username, Long sessionId, String currentSessionToken) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new AuthException("AUTHENTICATION_REQUIRED", "Authentication required");
        }

        Optional<UserSession> sessionOptional = em.createQuery(
                "SELECT s FROM UserSession s WHERE s.id = :sessionId AND s.userId = :userId " +
                    "AND s.revoked = false", UserSession.class)
            .setParameter("sessionId", sessionId)
            .setParameter("userId", user.getId())
            .getResultStream()
            .findFirst();

        if (sessionOptional.isEmpty()) {
            return null;
        }

        UserSession session = sessionOptional.get();
        session.setRevoked(true);
        return session.getId().equals(findSessionIdForToken(currentSessionToken));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        String username = getCurrentUsername();
        User user = userService.getUserByUsername(username);

        if (user == null) {
            throw new AuthException("AUTHENTICATION_REQUIRED", "Authentication required");
        }

        if (!BcryptUtil.matches(request.getCurrentPassword(), user.getPassword())) {
            Log.warnf("Password change failed: invalid current password for user: %s", username);
            throw new AuthException("CURRENT_PASSWORD_INCORRECT", "Current password is incorrect");
        }

        if (BcryptUtil.matches(request.getNewPassword(), user.getPassword())) {
            throw new AuthException(Response.Status.BAD_REQUEST, "PASSWORD_UNCHANGED", "New password must be different from current password");
        }

        user.setPassword(BcryptUtil.bcryptHash(request.getNewPassword()));

        em.createQuery("UPDATE UserSession s SET s.revoked = true WHERE s.userId = :userId")
            .setParameter("userId", user.getId())
            .executeUpdate();

        Log.infof("Password reset completed for user: %s", username);
    }

    @Transactional
    public void deleteAccount() {
        String username = getCurrentUsername();
        User user = userService.getUserByUsername(username);

        if (user == null) {
            throw new AuthException("AUTHENTICATION_REQUIRED", "Authentication required");
        }

        em.createQuery("DELETE FROM SessionCredential c WHERE c.sessionId IN " +
                "(SELECT s.id FROM UserSession s WHERE s.userId = :userId)")
            .setParameter("userId", user.getId())
            .executeUpdate();
        em.createQuery("DELETE FROM UserSession s WHERE s.userId = :userId")
            .setParameter("userId", user.getId())
            .executeUpdate();

        em.remove(user);

        Log.infof("User account deleted: %s", username);
    }

    private String generateSessionTokenValue() {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashSessionToken(String sessionToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = messageDigest.digest(sessionToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new AuthException(Response.Status.INTERNAL_SERVER_ERROR, "TOKEN_HASHING_UNAVAILABLE", "Session token hashing algorithm is not available");
        }
    }

    private SessionTokens createSessionTokens(User user, UserSession session, String sessionToken) {
        String accessToken = Jwt.issuer(jwtIssuer)
            .upn(user.getUsername())
            .groups(Set.of(DEFAULT_USER_ROLE))
            .claim("sid", session.getId())
            .expiresIn(Duration.ofMinutes(15))
            .sign();
        return new SessionTokens(accessToken, sessionToken);
    }

    private SessionResponse toSessionResponse(UserSession session, boolean current) {
        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setUserAgent(session.getUserAgent());
        response.setCreatedAt(session.getCreatedAt());
        response.setLastUsedAt(session.getLastUsedAt());
        response.setExpiresAt(session.getExpiresAt());
        response.setCurrent(current);
        return response;
    }

    private Long findSessionIdForToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return null;
        }
        return em.createNamedQuery("SessionCredential.findByTokenHash", SessionCredential.class)
            .setParameter("tokenHash", hashSessionToken(sessionToken))
            .getResultStream()
            .findFirst()
            .map(SessionCredential::getSessionId)
            .orElse(null);
    }

    private AuthException invalidSessionToken() {
        return new AuthException("INVALID_SESSION_TOKEN", "Session is invalid or expired");
    }

    private String limitUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() <= 512 ? userAgent : userAgent.substring(0, 512);
    }

    public String getCurrentUsername() {
        if (securityContext.getUserPrincipal() == null) {
            Log.warn("Authentication required but not provided");
            throw new AuthException("AUTHENTICATION_REQUIRED", "Authentication required");
        }
        
        String username = securityContext.getUserPrincipal().getName();
        User user = userService.getUserByUsername(username);
        
        if (user != null && user.isLocked()) {
            Log.warnf("Access denied: user account is locked: %s", username);
            throw new AuthException("ACCOUNT_LOCKED", "Account is locked");
        }
        
        return username;
    }
}