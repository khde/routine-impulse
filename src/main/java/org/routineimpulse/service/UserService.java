package org.routineimpulse.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.routineimpulse.model.User;
import org.routineimpulse.dto.UserProfile;

@ApplicationScoped
public class UserService {

    @Inject
    EntityManager em;

    @Inject
    AuthService authService;

    @Transactional
    public User createUser(User user) {
        em.persist(user);
        return user;
    }

    public User getUserByUsername(String username) {
        return em.createNamedQuery("User.findByUsername", User.class)
                 .setParameter("username", username)
                 .getResultList()
                 .stream()
                 .findFirst()
                 .orElse(null);
    }

    public UserProfile getCurrentProfile() {
        String currentUsername = authService.getCurrentUsername();

        User user = getUserByUsername(currentUsername);

        if (user == null) {
            return null;
        }

        UserProfile profile = new UserProfile();
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setCreationDate(user.getCreationDate());

        return profile;
    }
}
