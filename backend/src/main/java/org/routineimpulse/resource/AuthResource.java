package org.routineimpulse.resource;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.POST;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.core.NewCookie;
import io.quarkus.security.Authenticated;

import org.routineimpulse.dto.LoginRequest;
import org.routineimpulse.dto.LoginResponse;
import org.routineimpulse.dto.ChangePasswordRequest;
import org.routineimpulse.dto.SignupRequest;
import org.routineimpulse.service.AuthService;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String ACCESS_COOKIE_NAME = "access_token";

    @Inject
    AuthService authService;

    @POST
    @Path("/signup")
    public Response signup(@Valid SignupRequest signupRequest) {
        LoginResponse response = authService.register(signupRequest);

        String refreshToken = authService.createRefreshToken(response.getUsername());
        String accessToken = authService.createAccessToken(refreshToken);

        NewCookie accessCookie = buildAccessCookie(accessToken);
        NewCookie refreshCookie = buildRefreshCookie(refreshToken);

        return Response.status(Response.Status.CREATED)
            .cookie(accessCookie, refreshCookie)
            .entity(response)
            .build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest loginRequest) {
        LoginResponse response = authService.authenticate(loginRequest);

        String refreshToken = authService.createRefreshToken(response.getUsername());
        String accessToken = authService.createAccessToken(refreshToken);

        NewCookie accessCookie = buildAccessCookie(accessToken);
        NewCookie refreshCookie = buildRefreshCookie(refreshToken);

        return Response.noContent()
            .cookie(accessCookie, refreshCookie)
            .build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam(REFRESH_COOKIE_NAME) String refreshToken) {
        String newAccessToken = authService.createAccessToken(refreshToken);
        NewCookie accessCookie = buildAccessCookie(newAccessToken);

        return Response.noContent()
            .cookie(accessCookie)
            .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam(REFRESH_COOKIE_NAME) String refreshToken) {
        authService.logout(refreshToken);

        NewCookie accessCookie = clearAccessCookie();
        NewCookie refreshCookie = clearRefreshCookie();

        return Response.noContent()
            .cookie(accessCookie, refreshCookie)
            .build();
    }

    @POST
    @Path("/change-password")
    @Authenticated
    public Response changePassword(@Valid ChangePasswordRequest changePasswordRequest) {
        authService.changePassword(changePasswordRequest);

        return Response.noContent().build();
    }

    private NewCookie buildAccessCookie(String token) {
        return new NewCookie.Builder(ACCESS_COOKIE_NAME)
            .value(token)
            .path("/api")
            .maxAge(15 * 60)
            .secure(false)
            .httpOnly(true)
            .build();
    }

    private NewCookie buildRefreshCookie(String token) {
        return new NewCookie.Builder(REFRESH_COOKIE_NAME)
            .value(token)
            .path("/api/auth")
            .maxAge(7 * 24 * 60 * 60)
            .secure(false)
            .httpOnly(true)
            .build();
    }

    private NewCookie clearAccessCookie() {
        return new NewCookie.Builder(ACCESS_COOKIE_NAME)
            .value("")
            .path("/api")
            .maxAge(0)
            .secure(false)
            .httpOnly(true)
            .build();
    }

    private NewCookie clearRefreshCookie() {
        return new NewCookie.Builder(REFRESH_COOKIE_NAME)
            .value("")
            .path("/api/auth")
            .maxAge(0)
            .secure(false)
            .httpOnly(true)
            .build();
    }
}
