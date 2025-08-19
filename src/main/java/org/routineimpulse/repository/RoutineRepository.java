package org.routineimpulse.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import org.routineimpulse.model.Routine;

public interface RoutineRepository extends CrudRepository<Routine, Long> {
    List<Routine> findByUserId(Long id);
    Optional<Routine> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
