package org.routineimpulse.service;

import java.util.Optional;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import org.routineimpulse.repository.RoutineRepository;
import org.routineimpulse.repository.CompletionRepository;
import org.routineimpulse.model.Routine;
import org.routineimpulse.model.Completion;

@Service
public class RoutineService {
    private final RoutineRepository routineRepository;
    private final CompletionRepository completionRepository;

    public RoutineService(RoutineRepository routineRepository, CompletionRepository completionRepository) {
        this.routineRepository = routineRepository;
        this.completionRepository = completionRepository;
    }

    public Routine getRoutine(Long id) {
        Optional<Routine> routine = routineRepository.findById(id);
        if (routine.isPresent()) {
            return routine.get();
        }
        else {
            return null;
        }
    }

    public ArrayList<Routine> getAllRoutines() {
        Iterable<Routine> iterable = routineRepository.findAll();
        ArrayList<Routine> routines = new ArrayList<>();
        iterable.forEach(routines::add);
        return routines;

    }
    public Routine createRoutine(Routine routine) {
        return routineRepository.save(routine);
    }

    public void deleteRoutine(Long id) {
        routineRepository.deleteById(id);
    }

    public ArrayList<Completion> getCompletionsByRoutineId(Long id) {
        ArrayList<Completion> completions = completionRepository.findByRoutineId(id);
        return completions;
    }
}

