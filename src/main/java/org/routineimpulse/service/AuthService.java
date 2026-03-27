package org.routineimpulse.service;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;

import org.routineimpulse.dto.LoginRequest;
import org.routineimpulse.dto.LoginResponse;
import org.routineimpulse.dto.SignupRequest;
import org.routineimpulse.model.User;

@ApplicationScoped
public class AuthService {

    @Inject
    UserService userService;

    @Transactional
    public LoginResponse register(SignupRequest request) {
        String normalizedUsername = request.getUsername().toLowerCase().trim();

        if (userService.getUserByUsername(normalizedUsername) != null) {
            throw new BadRequestException("Username already in use");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(request.getEmail());

        String encryptedPassword = BcryptUtil.bcryptHash(request.getPassword());
        user.setPassword(encryptedPassword);

        userService.createUser(user);

        String jwt = issueJwt(user.getUsername());

        LoginResponse response = new LoginResponse();
        response.setUsername(user.getUsername());
        response.setToken(jwt);

        return response;
    }

    public LoginResponse authenticate(LoginRequest request) {
        User user = userService.getUserByUsername(request.getUsername());

        if (user == null || !BcryptUtil.matches(request.getPassword(), user.getPassword())) {
            throw new NotAuthorizedException("Invalid credentials");
        }

        String username = user.getUsername();

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
}
