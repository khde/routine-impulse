package org.routineimpulse.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;

import org.routineimpulse.service.AuthenticationService;
import org.routineimpulse.service.JwtService;
import org.routineimpulse.model.User;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private AuthenticationService authenticationService;
    private JwtService jwtService;

    public AuthenticationController(AuthenticationService authenticationService, JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    @GetMapping("/signup")
    public ResponseEntity<?> signup(@RequestParam String email, @RequestParam String username, @RequestParam String password) {
        return ResponseEntity.ok(authenticationService.signup(email, username, password));
    }

    @GetMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        User authenticatedUser = authenticationService.login(username, password);

        String jwtToken = jwtService.generateToken(authenticatedUser);
        // LoginResponse loginResponse = new LoginResponse().setToken(jwtToken).setExpiresIn(jwtService.getExpirationTime());
        return ResponseEntity.ok(jwtToken);
    }
}
