package org.routineimpulse.resource;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class JwtKeyTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            return Map.of(
                "JWT_PRIVATE_KEY", encoder.encodeToString(keyPair.getPrivate().getEncoded()),
                "JWT_PUBLIC_KEY", encoder.encodeToString(keyPair.getPublic().getEncoded())
            );
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to generate JWT test keys", e);
        }
    }

    @Override
    public void stop() {
    }
}
