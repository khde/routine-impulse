package org.routineimpulse.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import org.routineimpulse.repository.UserRepository;
import org.routineimpulse.model.User;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService (UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User getUserById(Long id) {
        Optional<User> routine = userRepository.findById(id);
        if (routine.isPresent()) {
            return routine.get();
        }
        else {
            return null;
        }
    }
    public User getUserByUsername(String username) {
        Optional<User> routine = userRepository.findByUsername(username);
        if (routine.isPresent()) {
            return routine.get();
        }
        else {
            return null;
        }
    }
}
