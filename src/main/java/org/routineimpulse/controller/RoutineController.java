package org.routineimpulse.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/routine")
class RoutineControlller {
    @GetMapping
    public ResponseEntity<?> getRoutines() {
        return ResponseEntity.ok("All");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoutine() {
        return ResponseEntity.ok("One");
    }

    @PostMapping
    public ResponseEntity<?> createRoutine() {
        return ResponseEntity.ok("Create");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoutine() {
        return ResponseEntity.ok("Delete");
    }

    @GetMapping("/activity")
    public ResponseEntity<?> getRoutinesActivity() {
        return ResponseEntity.ok("Activity");
    }
}

