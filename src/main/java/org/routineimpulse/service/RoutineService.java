package org.routineimpulse.service;

import java.util.Optional;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import org.routineimpulse.repository.RoutineRepository;
import org.routineimpulse.model.Routine;

@Service
public class RoutineService {
    private final RoutineRepository routineRepository;

    public RoutineService(RoutineRepository routineRepository) {
        this.routineRepository = routineRepository;
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
}

