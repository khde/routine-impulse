package org.routineimpulse.resource;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.POST;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.validation.Valid;

import org.routineimpulse.dto.LoginRequest;
import org.routineimpulse.dto.LoginResponse;
import org.routineimpulse.dto.SignupRequest;
import org.routineimpulse.service.AuthService;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/signup")
    public Response signup(@Valid SignupRequest signupRequest) {
        LoginResponse response= authService.register(signupRequest);
        
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest loginRequest) {
        LoginResponse response = authService.authenticate(loginRequest);
        
        return Response.ok(response).build();
    }
}
