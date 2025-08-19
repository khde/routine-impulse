package org.routineimpulse.service;

import java.util.Optional;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import org.routineimpulse.repository.RoutineRepository;
import org.routineimpulse.repository.CompletionRepository;
import org.routineimpulse.model.Routine;
import org.routineimpulse.model.Completion;
import org.routineimpulse.model.User;

@Service
public class RoutineService {
    private final RoutineRepository routineRepository;
    private final CompletionRepository completionRepository;
    private final UserService userService;

    public RoutineService(RoutineRepository routineRepository,
        CompletionRepository completionRepository,
        UserService userService
    ) {
        this.routineRepository = routineRepository;
        this.completionRepository = completionRepository;
        this.userService = userService;
    }

    public Routine getRoutine(Long id) {
        User user = userService.getCurrentUser();
        Optional<Routine> routine = routineRepository.findByIdAndUserId(id, user.getId());

        if (routine.isPresent()) {
            return routine.get();
        }
        else {
            return null;
        }
    }

    public ArrayList<Routine> getAllRoutines() {
        User user = userService.getCurrentUser();
        Iterable<Routine> iterable = routineRepository.findByUserId(user.getId());
        ArrayList<Routine> routines = new ArrayList<>();

        iterable.forEach(routines::add);
        return routines;

    }
    public Routine createRoutine(Routine routine) {
        User user = userService.getCurrentUser();
        routine.setUser(user);

        return routineRepository.save(routine);
    }

    public void deleteRoutine(Long id) {
        User user = userService.getCurrentUser();
        routineRepository.deleteByIdAndUserId(id, user.getId());
    }

    public ArrayList<Completion> getCompletionsByRoutineId(Long id) {
        ArrayList<Completion> completions = completionRepository.findByRoutineId(id);
        return completions;
    }
}

