package org.routineimpulse.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import io.quarkus.elytron.security.common.BcryptUtil;

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
        if (userService.getUserByUsername(request.getUsername()) != null) {
            throw new BadRequestException("Username already in use");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        String encryptedPassword = BcryptUtil.bcryptHash(request.getPassword());
        user.setPassword(encryptedPassword);

        userService.createUser(user);

        LoginResponse response = new LoginResponse();
        response.setUsername(user.getUsername());

        return response;
    }

    public LoginResponse authenticate(LoginRequest request) {
        User user = userService.getUserByUsername(request.getUsername());

        if (user == null || !BcryptUtil.matches(request.getPassword(), user.getPassword())) {
            throw new NotAuthorizedException("Invalid credentials");
        }

        LoginResponse response = new LoginResponse();
        response.setUsername(request.getUsername());
        
        return response;
    }
}
