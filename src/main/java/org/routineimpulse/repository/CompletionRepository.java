package org.routineimpulse.repository;

import java.util.ArrayList;

import org.springframework.data.repository.CrudRepository;

import org.routineimpulse.model.Completion;

public interface CompletionRepository extends CrudRepository<Completion, Long> {
        ArrayList<Completion> findByRoutineId(Long id);
}
