package org.routineimpulse.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.routineimpulse.model.User;

@ApplicationScoped
public class UserService {

    @Inject
    EntityManager em;

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
}
