package org.routineimpulse.service;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.core.SecurityContext;
import io.quarkus.logging.Log;

import org.routineimpulse.dto.LoginRequest;
import org.routineimpulse.dto.LoginResponse;
import org.routineimpulse.dto.SignupRequest;
import org.routineimpulse.model.User;

@ApplicationScoped
public class AuthService {

    @Inject
    UserService userService;

    @Inject
    SecurityContext securityContext;

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

        String encryptedPassword = BcryptUtil.bcryptHash(request.getPassword());
        user.setPassword(encryptedPassword);

        userService.createUser(user);
        Log.infof("New user registered: %s", normalizedUsername);

        String jwt = issueJwt(user.getUsername());

        LoginResponse response = new LoginResponse();
        response.setUsername(user.getUsername());
        response.setToken(jwt);

        return response;
    }

    public LoginResponse authenticate(LoginRequest request) {
        User user = userService.getUserByUsername(request.getUsername());

        if (user == null || !BcryptUtil.matches(request.getPassword(), user.getPassword())) {
            Log.warnf("Authentication failed: invalid credentials for user: %s", request.getUsername());
            throw new NotAuthorizedException("Invalid credentials");
        }

        String username = user.getUsername();
        Log.infof("User authenticated: %s", username);

        String jwt = issueJwt(username);

        LoginResponse response = new LoginResponse();
        response.setUsername(username);
        response.setToken(jwt);
        
        return response;
    }

    public String issueJwt(String username) {
        return Jwt.issuer("routineimpulse")
            .upn(username)
            .expiresIn(Duration.ofMinutes(120))
            .sign();
    }

    public String getCurrentUsername() {
        if (securityContext.getUserPrincipal() == null) {
            Log.warn("Authentication required but not provided");
            throw new NotAuthorizedException("Authentication required");
        }
        return securityContext.getUserPrincipal().getName();
    }
}
