package org.routineimpulse.resource;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.NewCookie;
import io.quarkus.security.Authenticated;

import org.routineimpulse.dto.LoginRequest;
import org.routineimpulse.dto.LoginResponse;
import org.routineimpulse.dto.ChangePasswordRequest;
import org.routineimpulse.dto.SessionTokens;
import org.routineimpulse.dto.SignupRequest;
import org.routineimpulse.service.AuthService;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final String SESSION_COOKIE_NAME = "session_token";
    private static final String ACCESS_COOKIE_NAME = "access_token";

    @Inject
    AuthService authService;

    @POST
    @Path("/signup")
    public Response signup(@Valid SignupRequest signupRequest, @HeaderParam("User-Agent") String userAgent) {
        LoginResponse response = authService.register(signupRequest);
        SessionTokens tokens = authService.createSession(response.getUsername(), userAgent);

        return Response.status(Response.Status.CREATED)
            .cookie(buildAccessCookie(tokens.getAccessToken()), buildSessionCookie(tokens.getSessionToken()))
            .entity(response)
            .build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest loginRequest, @HeaderParam("User-Agent") String userAgent) {
        LoginResponse response = authService.authenticate(loginRequest);
        SessionTokens tokens = authService.createSession(response.getUsername(), userAgent);

        return Response.noContent()
            .cookie(buildAccessCookie(tokens.getAccessToken()), buildSessionCookie(tokens.getSessionToken()))
            .build();
    }

    @POST
    @Path("/refresh")
    public Response refresh(@CookieParam(SESSION_COOKIE_NAME) String sessionToken) {
        SessionTokens tokens = authService.rotateSessionToken(sessionToken);

        return Response.noContent()
            .cookie(buildAccessCookie(tokens.getAccessToken()), buildSessionCookie(tokens.getSessionToken()))
            .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam(SESSION_COOKIE_NAME) String sessionToken) {
        authService.logoutSession(sessionToken);

        NewCookie accessCookie = clearAccessCookie();
        NewCookie sessionCookie = clearSessionCookie();

        return Response.noContent()
            .cookie(accessCookie, sessionCookie)
            .build();
    }

    @GET
    @Path("/sessions")
    @Authenticated
    public Response getSessions(@CookieParam(SESSION_COOKIE_NAME) String sessionToken) {
        return Response.ok(authService.getSessions(authService.getCurrentUsername(), sessionToken)).build();
    }

    @DELETE
    @Path("/sessions/{id}")
    @Authenticated
    public Response revokeSession(@PathParam("id") Long sessionId,
            @CookieParam(SESSION_COOKIE_NAME) String sessionToken) {
        Boolean current = authService.revokeSession(authService.getCurrentUsername(), sessionId, sessionToken);
        if (current == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (current) {
            return Response.noContent().cookie(clearAccessCookie(), clearSessionCookie()).build();
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/change-password")
    @Authenticated
    public Response changePassword(@Valid ChangePasswordRequest changePasswordRequest) {
        authService.changePassword(changePasswordRequest);

        return Response.noContent().cookie(clearAccessCookie(), clearSessionCookie()).build();
    }

    @DELETE
    @Path("/delete-account")
    @Authenticated
    public Response deleteAccount() {
        authService.deleteAccount();

        NewCookie accessCookie = clearAccessCookie();
        NewCookie sessionCookie = clearSessionCookie();

        return Response.noContent()
            .cookie(accessCookie, sessionCookie)
            .build();
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

    private NewCookie buildSessionCookie(String token) {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
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

    private NewCookie clearSessionCookie() {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
            .value("")
            .path("/api/auth")
            .maxAge(0)
            .secure(false)
            .httpOnly(true)
            .build();
    }
}
