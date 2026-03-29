package org.routineimpulse.service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import org.routineimpulse.dto.RoutineRequest;
import org.routineimpulse.dto.RoutineResponse;
import org.routineimpulse.model.Routine;
import org.routineimpulse.model.RoutineSchedule;
import org.routineimpulse.model.User;

@ApplicationScoped
public class RoutineService {

    @Inject
    EntityManager em;

    @Transactional
    public RoutineResponse createRoutine(RoutineRequest request, String username) {
        Routine routine = new Routine();
        routine.setName(request.getName());
        routine.setDescription(request.getDescription());

        routine.setUser(findUserByUsername(username));

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
        Routine routine = em.createQuery(
                "SELECT r FROM Routine r WHERE r.id = :id AND r.user.username = :username", Routine.class)
                .setParameter("id", id)
                .setParameter("username", username)
                .getSingleResult();

        if (routine == null) {
            throw new NotFoundException("Routine not found");
        }

        return mapToResponse(routine);
    }

    @Transactional
    public void deleteRoutine(Long id, String username) {
        Routine routine = em.createQuery(
                "SELECT r FROM Routine r WHERE r.id = :id AND r.user.username = :username", Routine.class)
                .setParameter("id", id)
                .setParameter("username", username)
                .getSingleResult();

        if (routine == null) {
            throw new NotFoundException("Routine not found");
        }

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

    private User findUserByUsername(String username) {
        return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getSingleResult();
    }
}
