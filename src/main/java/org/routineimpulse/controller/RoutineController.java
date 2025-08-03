package org.routineimpulse.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.http.ResponseEntity;

import org.routineimpulse.service.RoutineService;
import org.routineimpulse.model.Routine;

@RestController
@RequestMapping("/api/routine")
class RoutineController {
    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @GetMapping
    public ResponseEntity<?> getRoutines() {
        return ResponseEntity.ok(routineService.getAllRoutines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoutine(@PathVariable Long id) {
        Routine routine = routineService.getRoutine(id);
        if (routine != null) {
            return ResponseEntity.ok(routine);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createRoutine() {
        return ResponseEntity.ok(routineService.createRoutine());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoutine(@PathVariable Long id) {
        routineService.deleteRoutine(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activity")
    public ResponseEntity<?> getRoutinesActivity() {
        return ResponseEntity.ok("Activity");
    }
}

