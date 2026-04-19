package org.routineimpulse.service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.core.Response;

import org.routineimpulse.dto.RoutineRequest;
import org.routineimpulse.dto.RoutineResponse;
import org.routineimpulse.dto.RoutineUpdateRequest;
import org.routineimpulse.exception.RoutineException;
import org.routineimpulse.model.Routine;
import org.routineimpulse.model.RoutineSchedule;
import org.routineimpulse.model.User;

@ApplicationScoped
public class RoutineService {

    @Inject
    EntityManager em;

    @Inject
    UserService userService;

    @Transactional
    public RoutineResponse createRoutine(RoutineRequest request, String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new RoutineException(Response.Status.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required");
        }

        Routine routine = new Routine();
        routine.setName(request.getName());
        routine.setDescription(request.getDescription());

        routine.setUser(user);

        RoutineSchedule schedule = new RoutineSchedule();
        if (request.getSelectedDays() != null) {
            for (var day : request.getSelectedDays()) {
                schedule.addDay(day);
            }
        }
        routine.setSchedule(schedule);

        em.persist(routine);

        return mapToResponse(routine);
    }

    public List<RoutineResponse> getAllRoutinesForUser(String username) {
        List<Routine> routines = em.createQuery(
                "SELECT r FROM Routine r WHERE r.user.username = :username", Routine.class)
                .setParameter("username", username)
                .getResultList();

        return routines.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RoutineResponse getRoutineById(Long id, String username) {
        Routine routine = findRoutineByIdAndUsername(id, username);
        return mapToResponse(routine);
    }

    @Transactional
    public RoutineResponse updateRoutine(Long id, RoutineUpdateRequest request, String username) {
        Routine routine = findRoutineByIdAndUsername(id, username);

        if (request.getName() != null) {
            routine.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            routine.setDescription(request.getDescription().trim());
        }

        if (request.getSelectedDays() != null) {
            RoutineSchedule schedule = routine.getSchedule();
            if (schedule == null) {
                schedule = new RoutineSchedule();
                routine.setSchedule(schedule);
            }

            schedule.clearDays();
            for (var day : request.getSelectedDays()) {
                schedule.addDay(day);
            }
        }

        return mapToResponse(routine);
    }

    @Transactional
    public void deleteRoutine(Long id, String username) {
        Routine routine = findRoutineByIdAndUsername(id, username);
        em.remove(routine);
    }

    private RoutineResponse mapToResponse(Routine routine) {
        RoutineResponse response = new RoutineResponse();
        response.setId(routine.getId());
        response.setName(routine.getName());
        response.setDescription(routine.getDescription());
        response.setCreationDate(routine.getCreationDate());

        if (routine.getSchedule() != null) {
            response.setSelectedDays(routine.getSchedule().getSelectedDays());
        }

        return response;
    }

    private Routine findRoutineByIdAndUsername(Long id, String username) {
        try {
            return em.createQuery(
                    "SELECT r FROM Routine r WHERE r.id = :id AND r.user.username = :username", Routine.class)
                    .setParameter("id", id)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new RoutineException(Response.Status.NOT_FOUND, "ROUTINE_NOT_FOUND", "Routine not found");
        }
    }
}
