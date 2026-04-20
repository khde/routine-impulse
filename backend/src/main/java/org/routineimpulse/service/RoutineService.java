package org.routineimpulse.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.core.Response;

import org.routineimpulse.dto.ActivityFilter;
import org.routineimpulse.dto.RoutineActivityResponse;
import org.routineimpulse.dto.RoutineActivityUpdateRequest;
import org.routineimpulse.dto.RoutineRequest;
import org.routineimpulse.dto.RoutineResponse;
import org.routineimpulse.dto.RoutineUpdateRequest;
import org.routineimpulse.exception.RoutineException;
import org.routineimpulse.model.RoutineActivity;
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

    public List<RoutineActivityResponse> getRoutineActivity(Long routineId, String username, String from, String to, ActivityFilter filter) {
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);

        if (fromDate.isAfter(toDate)) {
            throw new RoutineException(Response.Status.BAD_REQUEST, "INVALID_DATE_RANGE", "From date must not be after to date");
        }

        Routine routine = findRoutineByIdAndUsername(routineId, username);
        List<RoutineActivity> activities = em.createQuery(
                "SELECT a FROM RoutineActivity a WHERE a.routine.id = :routineId AND a.activityDate BETWEEN :from AND :to ORDER BY a.activityDate",
                RoutineActivity.class)
                .setParameter("routineId", routineId)
                .setParameter("from", fromDate)
                .setParameter("to", toDate)
                .getResultList();

        List<RoutineActivityResponse> responses = new ArrayList<>();
        Set<DayOfWeek> selectedDays = routine.getSchedule() != null ? routine.getSchedule().getSelectedDays() : Set.of();

        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            boolean scheduled = selectedDays.contains(current.getDayOfWeek());
            if (!scheduled) {
                current = current.plusDays(1);
                continue;
            }

            RoutineActivity activity = findActivityForDate(activities, current);
            boolean completed = activity != null && activity.isCompleted();

            if (filter == ActivityFilter.COMPLETED && !completed) {
                current = current.plusDays(1);
                continue;
            }

            if (filter == ActivityFilter.UNCOMPLETED && completed) {
                current = current.plusDays(1);
                continue;
            }

            RoutineActivityResponse response = new RoutineActivityResponse();
            response.setDate(current);
            response.setCompleted(completed);
            response.setUpdatedDate(activity != null ? activity.getUpdatedDate() : null);
            responses.add(response);

            current = current.plusDays(1);
        }

        return responses;
    }

    @Transactional
    public RoutineActivityResponse markRoutineActivity(Long routineId, String username, RoutineActivityUpdateRequest request) {
        LocalDate activityDate = parseDate(request.getDate());

        if (activityDate.isAfter(LocalDate.now())) {
            throw new RoutineException(Response.Status.BAD_REQUEST, "INVALID_ACTIVITY_DATE", "Activity date must not be in the future");
        }

        Routine routine = findRoutineByIdAndUsername(routineId, username);

        if (!routine.getSchedule().getSelectedDays().contains(activityDate.getDayOfWeek())) {
            throw new RoutineException(Response.Status.BAD_REQUEST, "INVALID_ACTIVITY_DAY", "Routine is not scheduled for the given date");
        }

        Optional<RoutineActivity> activityOptional = em.createQuery(
                "SELECT a FROM RoutineActivity a WHERE a.routine.id = :routineId AND a.activityDate = :date",
                RoutineActivity.class)
                .setParameter("routineId", routineId)
                .setParameter("date", activityDate)
                .getResultStream()
                .findFirst();

        RoutineActivity activity = activityOptional.orElseGet(() -> {
            RoutineActivity newActivity = new RoutineActivity();
            newActivity.setRoutine(routine);
            newActivity.setActivityDate(activityDate);
            em.persist(newActivity);
            return newActivity;
        });

        activity.setCompleted(request.isCompleted());

        return mapToActivityResponse(activity);
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

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            throw new RoutineException(Response.Status.BAD_REQUEST, "INVALID_DATE_FORMAT", "Dates must use YYYY-MM-DD format");
        }

        try {
            return LocalDate.parse(rawDate);
        } catch (DateTimeParseException e) {
            throw new RoutineException(Response.Status.BAD_REQUEST, "INVALID_DATE_FORMAT", "Dates must use YYYY-MM-DD format");
        }
    }

    private RoutineActivity findActivityForDate(List<RoutineActivity> activities, LocalDate date) {
        return activities.stream()
                .filter(activity -> date.equals(activity.getActivityDate()))
                .findFirst()
                .orElse(null);
    }

    private RoutineActivityResponse mapToActivityResponse(RoutineActivity activity) {
        RoutineActivityResponse response = new RoutineActivityResponse();
        response.setDate(activity.getActivityDate());
        response.setCompleted(activity.isCompleted());
        response.setUpdatedDate(activity.getUpdatedDate());
        return response;
    }
}
