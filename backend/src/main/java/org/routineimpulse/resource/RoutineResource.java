package org.routineimpulse.resource;

import java.util.List;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.PathParam;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Context;

import org.routineimpulse.dto.ActivityFilter;
import org.routineimpulse.dto.RoutineActivityResponse;
import org.routineimpulse.dto.RoutineActivityUpdateRequest;
import org.routineimpulse.dto.RoutineRequest;
import org.routineimpulse.dto.RoutineResponse;
import org.routineimpulse.dto.RoutineUpdateRequest;
import org.routineimpulse.service.RoutineService;
import org.routineimpulse.service.AuthService;

@Path("/routine")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoutineResource {

    @Inject
    RoutineService routineService;

    @Context
    AuthService authService;

    @POST
    public Response createRoutine(@Valid RoutineRequest request) {
        String username = authService.getCurrentUsername();
        RoutineResponse response = routineService.createRoutine(request, username);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/all")
    public Response getAllRoutinesForUser() {
        String username = authService.getCurrentUsername();
        List<RoutineResponse> routines = routineService.getAllRoutinesForUser(username);

        return Response.ok(routines).build();
    }

    @GET
    @Path("/{id}")
    public Response getRoutineById(@PathParam("id") Long id) {
        String username = authService.getCurrentUsername();
        RoutineResponse response = routineService.getRoutineById(id, username);

        return Response.ok(response).build();
    }

    @PATCH
    @Path("/{id}")
    public Response updateRoutine(@PathParam("id") Long id, @Valid RoutineUpdateRequest request) {
        String username = authService.getCurrentUsername();
        RoutineResponse response = routineService.updateRoutine(id, request, username);

        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRoutine(@PathParam("id") Long id) {
        String username = authService.getCurrentUsername();
        routineService.deleteRoutine(id, username);

        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/activity")
    public Response getRoutineActivity(@PathParam("id") Long id,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @DefaultValue("ALL") @QueryParam("status") ActivityFilter status) {
        String username = authService.getCurrentUsername();
        List<RoutineActivityResponse> activity = routineService.getRoutineActivity(
            id,
            username,
            from,
            to,
            status);

        return Response.ok(activity).build();
    }

    @PUT
    @Path("/{id}/activity")
    public Response markRoutineActivity(@PathParam("id") Long id, @Valid RoutineActivityUpdateRequest request) {
        String username = authService.getCurrentUsername();
        RoutineActivityResponse activity = routineService.markRoutineActivity(id, username, request);

        return Response.ok(activity).build();
    }

    @GET
    @Path("/{id}/activity/stats")
    public Response getRoutineActivityStats(@PathParam("id") Long id) {
        return Response.status(Response.Status.NOT_IMPLEMENTED)
            .entity("Not implemented")
            .build();
    }
}