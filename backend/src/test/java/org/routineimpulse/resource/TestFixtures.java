package org.routineimpulse.resource;

import java.util.UUID;

final class TestFixtures {

    private TestFixtures() {
    }

    static String uniqueUsername() {
        return "t" + UUID.randomUUID().toString().substring(0, 19);
    }
}