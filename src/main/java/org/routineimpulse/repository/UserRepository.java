package org.routineimpulse.repository;

import org.springframework.data.repository.CrudRepository;

import org.routineimpulse.model.User;

public interface UserRepository extends CrudRepository<User, Long> {
}
