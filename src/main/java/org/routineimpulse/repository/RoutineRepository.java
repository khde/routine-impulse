package org.routineimpulse.repository;

import org.springframework.data.repository.CrudRepository;

import org.routineimpulse.model.Routine;

public interface RoutineRepository extends CrudRepository<Routine, Long> {
}
