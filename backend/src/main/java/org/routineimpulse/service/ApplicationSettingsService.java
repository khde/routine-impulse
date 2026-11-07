package org.routineimpulse.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.routineimpulse.model.ApplicationSetting;

@ApplicationScoped
public class ApplicationSettingsService {

    private static final String SIGNUP_ENABLED_KEY = "signup_enabled";

    @Inject
    EntityManager em;

    @Transactional
    public void initializeDefaults() {
        boolean settingMissing = em.createQuery(
                "SELECT s.key FROM ApplicationSetting s WHERE s.key = :key", String.class)
            .setParameter("key", SIGNUP_ENABLED_KEY)
            .getResultList()
            .isEmpty();
        if (settingMissing) {
            em.persist(new ApplicationSetting(SIGNUP_ENABLED_KEY, "true"));
        }
    }

    public boolean isSignupEnabled() {
        ApplicationSetting setting = em.find(ApplicationSetting.class, SIGNUP_ENABLED_KEY);
        return setting == null || Boolean.parseBoolean(setting.getValue());
    }
}