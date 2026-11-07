package org.routineimpulse.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ApplicationSettingsInitializer {

    @Inject
    ApplicationSettingsService settingsService;

    void onStart(@Observes StartupEvent event) {
        settingsService.initializeDefaults();
    }
}