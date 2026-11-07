package org.routineimpulse.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.routineimpulse.resource.TestFixtures.uniqueUsername;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.routineimpulse.model.ApplicationSetting;

@QuarkusTest
@QuarkusTestResource(JwtKeyTestResource.class)
class AuthResourceTest {

    @Inject
    EntityManager em;

    @Test
    void signupCanBeDisabledThroughDatabaseSetting() {
        String username = uniqueUsername();
        String password = "test-password-123";
        Response signup = signup(username, username + "test@example.test", password);
        signup.then().statusCode(201);
        String accessToken = signup.getCookie("access_token");

        try {
            setSignupEnabled(false);

            given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "username", uniqueUsername(),
                    "email", "disabled-signup@example.test",
                    "password", "test-password-123"
                ))
            .when()
                .post("/api/auth/signup")
            .then()
                .statusCode(403)
                .body("message", equalTo("New account registration is currently disabled"));

            given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
            .when()
                .post("/api/auth/login")
            .then()
                .statusCode(204);
        } finally {
            setSignupEnabled(true);
            given()
                .cookie("access_token", accessToken)
            .when()
                .delete("/api/auth/delete-account")
            .then()
                .statusCode(204);
        }
    }

    @Test
    void signupCreatesSessionAndDeleteAccountRemovesUser() {
        String username = uniqueUsername();
        String email = username + "test@example.test";
        String password = "test-password-123";

        Response signup = signup(username, email, password);
        signup.then()
            .statusCode(201)
            .body("username", equalTo(username));

        String accessToken = signup.getCookie("access_token");
        assertNotNull(accessToken);
        assertFalse(accessToken.isBlank());
        assertNotNull(signup.getCookie("session_token"));

        signup(username, email, password).then()
            .statusCode(400);

        given()
            .cookie("access_token", accessToken)
        .when()
            .delete("/api/auth/delete-account")
        .then()
            .statusCode(204);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("username", username, "password", password))
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(401);
    }

    @Test
    void signupRejectsInvalidInput() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", uniqueUsername(),
                "email", "invalid-email",
                "password", "short"
            ))
        .when()
            .post("/api/auth/signup")
        .then()
            .statusCode(400);
    }

    @Test
    void loginRejectsIncorrectPasswordForExistingUsername() {
        String username = uniqueUsername();
        Response signup = signup(username, username + "test@example.test", "test-password-123");
        signup.then().statusCode(201);

        String accessToken = signup.getCookie("access_token");
        assertNotNull(accessToken);

        try {
            given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", "wrong-password"))
            .when()
                .post("/api/auth/login")
            .then()
                .statusCode(401);
        } finally {
            given()
                .cookie("access_token", accessToken)
            .when()
                .delete("/api/auth/delete-account")
            .then()
                .statusCode(204);
        }
    }

    @Test
    void refreshRotatesSessionCredentialAndRejectsReuse() {
        String username = uniqueUsername();
        String email = username + "test@example.test";
        String password = "test-password-123";
        Response signup = signup(username, email, password);
        signup.then().statusCode(201);

        String sessionToken = signup.getCookie("session_token");
        String signupAccessToken = signup.getCookie("access_token");
        assertNotNull(sessionToken);
        assertNotNull(signupAccessToken);

        try {
            Response refresh = given()
                .contentType(ContentType.JSON)
                .cookie("session_token", sessionToken)
            .when()
                .post("/api/auth/refresh");
            refresh.then().statusCode(204);

            String refreshedAccessToken = refresh.getCookie("access_token");
            String rotatedSessionToken = refresh.getCookie("session_token");
            assertNotNull(refreshedAccessToken);
            assertNotNull(rotatedSessionToken);
            assertFalse(sessionToken.equals(rotatedSessionToken));
            assertFalse(refreshedAccessToken.isBlank());

            given()
                .cookie("access_token", refreshedAccessToken)
            .when()
                .get("/api/user/account")
            .then()
                .statusCode(200)
                .body("username", equalTo(username));

            given()
                .contentType(ContentType.JSON)
                .cookie("session_token", sessionToken)
            .when()
                .post("/api/auth/refresh")
            .then()
                .statusCode(401);

            given()
                .contentType(ContentType.JSON)
                .cookie("session_token", rotatedSessionToken)
            .when()
                .post("/api/auth/refresh")
            .then()
                .statusCode(401);
        } finally {
            given()
                .cookie("access_token", signupAccessToken)
            .when()
                .delete("/api/auth/delete-account")
            .then()
                .statusCode(204);
        }
    }

    @Test
    void refreshRejectsMissingRefreshToken() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(401);
    }

    @Test
    void refreshRejectsInvalidRefreshToken() {
        given()
            .contentType(ContentType.JSON)
            .cookie("session_token", "invalid-session-token")
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(401);
    }

    @Test
    void logoutRevokesSessionCredential() {
        String username = uniqueUsername();
        Response signup = signup(username, username + "test@example.test", "test-password-123");
        signup.then().statusCode(201);

        String sessionToken = signup.getCookie("session_token");
        String accessToken = signup.getCookie("access_token");
        assertNotNull(sessionToken);
        assertNotNull(accessToken);

        try {
            given()
                .contentType(ContentType.JSON)
                .cookie("session_token", sessionToken)
            .when()
                .post("/api/auth/logout")
            .then()
                .statusCode(204);

            given()
                .contentType(ContentType.JSON)
                .cookie("session_token", sessionToken)
            .when()
                .post("/api/auth/refresh")
            .then()
                .statusCode(401);

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/user/account")
            .then()
                .statusCode(200);
        } finally {
            given()
                .cookie("access_token", accessToken)
            .when()
                .delete("/api/auth/delete-account")
            .then()
                .statusCode(204);
        }
    }

    @Test
    void loginCreatesIndependentSessionAndCanRevokeAnotherSession() {
        String username = uniqueUsername();
        String password = "test-password-123";
        Response signup = signup(username, username + "test@example.test", password);
        signup.then().statusCode(201);
        String firstSessionToken = signup.getCookie("session_token");

        Response login = given()
            .contentType(ContentType.JSON)
            .header("User-Agent", "Second Test Browser")
            .body(Map.of("username", username, "password", password))
        .when()
            .post("/api/auth/login");
        login.then().statusCode(204);
        String secondSessionToken = login.getCookie("session_token");
        String secondAccessToken = login.getCookie("access_token");
        assertNotNull(secondSessionToken);
        assertNotNull(secondAccessToken);

        try {
            given()
                .cookie("access_token", secondAccessToken)
                .cookie("session_token", secondSessionToken)
            .when()
                .get("/api/auth/sessions")
            .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("find { it.current }.userAgent", equalTo("Second Test Browser"));

            Long firstSessionId = given()
                .cookie("access_token", secondAccessToken)
                .cookie("session_token", secondSessionToken)
            .when()
                .get("/api/auth/sessions")
            .then()
                .extract()
                .jsonPath()
                .getLong("find { !it.current }.id");

            given()
                .cookie("access_token", secondAccessToken)
                .cookie("session_token", secondSessionToken)
            .when()
                .delete("/api/auth/sessions/" + firstSessionId)
            .then()
                .statusCode(204);

            given()
                .contentType(ContentType.JSON)
                .cookie("session_token", firstSessionToken)
            .when()
                .post("/api/auth/refresh")
            .then()
                .statusCode(401);
        } finally {
            given()
                .cookie("access_token", secondAccessToken)
            .when()
                .delete("/api/auth/delete-account")
            .then()
                .statusCode(204);
        }
    }

    private Response signup(String username, String email, String password) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", username,
                "email", email,
                "password", password
            ))
        .when()
            .post("/api/auth/signup");
    }

    private void setSignupEnabled(boolean enabled) {
        QuarkusTransaction.requiringNew().run(() -> {
            ApplicationSetting setting = em.find(ApplicationSetting.class, "signup_enabled");
            setting.setValue(Boolean.toString(enabled));
        });
    }
}