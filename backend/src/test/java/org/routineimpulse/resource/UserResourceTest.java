package org.routineimpulse.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
class UserResourceTest {

    @Test
    void getAccountReturnsCurrentAuthenticatedUser() {
        String username = uniqueUsername();
        String email = username + "@example.test";

        Response signup = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", username,
                "email", email,
                "password", "test-password-123"
            ))
        .when()
            .post("/api/auth/signup");
        signup.then()
            .statusCode(201)
            .body("username", equalTo(username));

        String accessToken = signup.getCookie("access_token");
        assertNotNull(accessToken);
        try {
            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/user/account")
            .then()
                .statusCode(200)
                .body("username", equalTo(username))
                .body("email", equalTo(email))
                .body("creationDate", notNullValue())
                .body("password", nullValue());
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
    void accountRequiresValidAuthentication() {
        given()
        .when()
            .get("/api/user/account")
        .then()
            .statusCode(401);

        given()
            .cookie("access_token", "not-a-valid-jwt")
        .when()
            .get("/api/user/account")
        .then()
            .statusCode(401);
    }

    private String uniqueUsername() {
        return "t" + UUID.randomUUID().toString().substring(0, 19);
    }
}