package org.routineimpulse.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.routineimpulse.resource.TestFixtures.uniqueUsername;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
class TaskResourceTest {

    @Test
    void taskCrudAndRequestValidation() {
        String username = uniqueUsername();
        String accessToken = createUser(username);

        try {
            Response created = given()
                .cookie("access_token", accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of("description", "Read a book"))
            .when()
                .post("/api/task");
            created.then()
                .statusCode(201)
                .body("description", equalTo("Read a book"))
                .body("completed", equalTo(false))
                .body("dueDate", nullValue())
                .body("creationDate", notNullValue());
            Long taskId = created.jsonPath().getLong("id");

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/task/all")
            .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(taskId.intValue()));

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/task/" + taskId)
            .then()
                .statusCode(200)
                .body("description", equalTo("Read a book"));

            String dueDate = LocalDateTime.now().plusDays(2).withNano(0).toString();
            given()
                .cookie("access_token", accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of("description", " Read two chapters ", "completed", true, "dueDate", dueDate))
            .when()
                .patch("/api/task/" + taskId)
            .then()
                .statusCode(200)
                .body("description", equalTo("Read two chapters"))
                .body("completed", equalTo(true))
                .body("dueDate", equalTo(dueDate));

            given()
                .cookie("access_token", accessToken)
            .when()
                .delete("/api/task/" + taskId)
            .then()
                .statusCode(204);

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/task/" + taskId)
            .then()
                .statusCode(404);

            rejectTask(accessToken, Map.of("description", ""));
            rejectTask(accessToken, Map.of("description", "x".repeat(256)));
            rejectTask(accessToken, Map.of(
                "description", "Past deadline",
                "dueDate", LocalDateTime.now().minusDays(1).withNano(0).toString()
            ));
        } finally {
            deleteUser(accessToken);
        }
    }

    @Test
    void usersCannotAccessAnotherUsersTask() {
        String ownerToken = createUser(uniqueUsername());
        String otherUserToken = createUser(uniqueUsername());

        try {
            Response created = given()
                .cookie("access_token", ownerToken)
                .contentType(ContentType.JSON)
                .body(Map.of("description", "Private task"))
            .when()
                .post("/api/task");
            created.then().statusCode(201);
            Long taskId = created.jsonPath().getLong("id");

            given()
                .cookie("access_token", otherUserToken)
            .when()
                .get("/api/task/" + taskId)
            .then()
                .statusCode(404);

            given()
                .cookie("access_token", otherUserToken)
                .contentType(ContentType.JSON)
                .body(Map.of("description", "Changed by another user"))
            .when()
                .patch("/api/task/" + taskId)
            .then()
                .statusCode(404);

            given()
                .cookie("access_token", otherUserToken)
            .when()
                .delete("/api/task/" + taskId)
            .then()
                .statusCode(404);

            given()
                .cookie("access_token", ownerToken)
            .when()
                .get("/api/task/" + taskId)
            .then()
                .statusCode(200)
                .body("description", equalTo("Private task"));
        } finally {
            deleteUser(ownerToken);
            deleteUser(otherUserToken);
        }
    }

    private String createUser(String username) {
        Response signup = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "username", username,
                "email", username + "@example.test",
                "password", "test-password-123"
            ))
        .when()
            .post("/api/auth/signup");
        signup.then().statusCode(201);
        String accessToken = signup.getCookie("access_token");
        assertNotNull(accessToken);
        return accessToken;
    }

    private void deleteUser(String accessToken) {
        given()
            .cookie("access_token", accessToken)
        .when()
            .delete("/api/auth/delete-account")
        .then()
            .statusCode(204);
    }

    private void rejectTask(String accessToken, Map<String, String> request) {
        given()
            .cookie("access_token", accessToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/task")
        .then()
            .statusCode(400);
    }
}