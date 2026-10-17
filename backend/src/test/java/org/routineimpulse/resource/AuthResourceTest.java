package org.routineimpulse.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
class AuthResourceTest {

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
        assertNotNull(signup.getCookie("refresh_token"));

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
    void refreshIssuesUsableAccessTokenWithoutRotatingRefreshToken() {
        String username = uniqueUsername();
        String email = username + "test@example.test";
        String password = "test-password-123";
        Response signup = signup(username, email, password);
        signup.then().statusCode(201);

        String refreshToken = signup.getCookie("refresh_token");
        String signupAccessToken = signup.getCookie("access_token");
        assertNotNull(refreshToken);
        assertNotNull(signupAccessToken);

        try {
            Response refresh = given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", refreshToken)
            .when()
                .post("/api/auth/refresh");
            refresh.then().statusCode(204);

            String refreshedAccessToken = refresh.getCookie("access_token");
            assertNotNull(refreshedAccessToken);
            assertFalse(refreshedAccessToken.isBlank());
            assertNull(refresh.getCookie("refresh_token"));

            given()
                .cookie("access_token", refreshedAccessToken)
            .when()
                .get("/api/user/account")
            .then()
                .statusCode(200)
                .body("username", equalTo(username));

            given()
                .contentType(ContentType.JSON)
                .cookie("refresh_token", refreshToken)
            .when()
                .post("/api/auth/refresh")
            .then()
                .statusCode(204);
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
            .cookie("refresh_token", "invalid-refresh-token")
        .when()
            .post("/api/auth/refresh")
        .then()
            .statusCode(401);
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

    private String uniqueUsername() {
        return "t" + UUID.randomUUID().toString().substring(0, 19);
    }
}