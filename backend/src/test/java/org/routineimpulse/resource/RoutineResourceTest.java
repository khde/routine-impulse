package org.routineimpulse.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@QuarkusTest
class RoutineResourceTest {

    @Test
    void routineCrudAndNameValidation() {
        String accessToken = createUser(uniqueUsername());

        try {
            Response created = createRoutine(accessToken, "Morning walk", Set.of("MONDAY", "WEDNESDAY"));
            created.then()
                .statusCode(201)
                .body("name", equalTo("Morning walk"))
                .body("selectedDays", hasSize(2))
                .body("creationDate", notNullValue());
            Long routineId = created.jsonPath().getLong("id");

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/routine/all")
            .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].id", equalTo(routineId.intValue()));

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/routine/" + routineId)
            .then()
                .statusCode(200)
                .body("name", equalTo("Morning walk"));

            given()
                .cookie("access_token", accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                    "name", " Evening walk ",
                    "description", " Updated description ",
                    "selectedDays", Set.of("FRIDAY")
                ))
            .when()
                .patch("/api/routine/" + routineId)
            .then()
                .statusCode(200)
                .body("name", equalTo("Evening walk"))
                .body("description", equalTo("Updated description"))
                .body("selectedDays", hasSize(1))
                .body("selectedDays[0]", equalTo("FRIDAY"));

            given()
                .cookie("access_token", accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "   "))
            .when()
                .patch("/api/routine/" + routineId)
            .then()
                .statusCode(400);

            given()
                .cookie("access_token", accessToken)
            .when()
                .delete("/api/routine/" + routineId)
            .then()
                .statusCode(204);

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/routine/" + routineId)
            .then()
                .statusCode(404);

            given()
                .cookie("access_token", accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "   ", "selectedDays", Set.of("MONDAY")))
            .when()
                .post("/api/routine")
            .then()
                .statusCode(400);
        } finally {
            deleteUser(accessToken);
        }
    }

    @Test
    void usersCannotAccessAnotherUsersRoutine() {
        String ownerToken = createUser(uniqueUsername());
        String otherUserToken = createUser(uniqueUsername());

        try {
            Response created = createRoutine(ownerToken, "Private routine", Set.of("MONDAY"));
            created.then().statusCode(201);
            Long routineId = created.jsonPath().getLong("id");

            given()
                .cookie("access_token", otherUserToken)
            .when()
                .get("/api/routine/" + routineId)
            .then()
                .statusCode(404);

            given()
                .cookie("access_token", otherUserToken)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Taken over"))
            .when()
                .patch("/api/routine/" + routineId)
            .then()
                .statusCode(404);

            given()
                .cookie("access_token", otherUserToken)
            .when()
                .delete("/api/routine/" + routineId)
            .then()
                .statusCode(404);

            given()
                .cookie("access_token", ownerToken)
            .when()
                .get("/api/routine/" + routineId)
            .then()
                .statusCode(200)
                .body("name", equalTo("Private routine"));
        } finally {
            deleteUser(ownerToken);
            deleteUser(otherUserToken);
        }
    }

    @Test
    void routineActivityCanBeMarkedReadAndFiltered() {
        String accessToken = createUser(uniqueUsername());
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String todayName = today.name();
        String todayDate = LocalDate.now().toString();

        try {
            Response created = createRoutine(accessToken, "Daily practice", Set.of(todayName));
            created.then().statusCode(201);
            Long routineId = created.jsonPath().getLong("id");

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/routine/" + routineId + "/activity?from=" + todayDate + "&to=" + todayDate)
            .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].date", equalTo(todayDate))
                .body("[0].completed", equalTo(false));

            given()
                .cookie("access_token", accessToken)
                .contentType(ContentType.JSON)
                .body(Map.of("date", todayDate, "completed", true))
            .when()
                .put("/api/routine/" + routineId + "/activity")
            .then()
                .statusCode(200)
                .body("date", equalTo(todayDate))
                .body("completed", equalTo(true));

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/routine/" + routineId + "/activity?from=" + todayDate + "&to=" + todayDate + "&status=COMPLETED")
            .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].completed", equalTo(true));

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/routine/" + routineId + "/activity?from=" + todayDate + "&to=" + todayDate + "&status=UNCOMPLETED")
            .then()
                .statusCode(200)
                .body("", hasSize(0));
        } finally {
            deleteUser(accessToken);
        }
    }

    @Test
    void routineActivityRejectsInvalidDatesAndUnscheduledDay() {
        String accessToken = createUser(uniqueUsername());

        try {
            Response created = createRoutine(accessToken, "Monday routine", Set.of("MONDAY"));
            created.then().statusCode(201);
            Long routineId = created.jsonPath().getLong("id");
            LocalDate today = LocalDate.now();
            LocalDate futureDate = today.plusDays(1);
            LocalDate preCreationDate = today.minusDays(7);
            LocalDate unscheduledDate = nextDateForDifferentDay(today, DayOfWeek.MONDAY);

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/routine/" + routineId + "/activity?from=bad-date&to=" + today)
            .then()
                .statusCode(400);

            given()
                .cookie("access_token", accessToken)
            .when()
                .get("/api/routine/" + routineId + "/activity?from=" + today + "&to=" + preCreationDate)
            .then()
                .statusCode(400);

            rejectActivityUpdate(accessToken, routineId, futureDate.toString(), true);
            rejectActivityUpdate(accessToken, routineId, preCreationDate.toString(), true);
            rejectActivityUpdate(accessToken, routineId, unscheduledDate.toString(), true);
            rejectActivityUpdate(accessToken, routineId, "bad-date", true);
        } finally {
            deleteUser(accessToken);
        }
    }

    private Response createRoutine(String accessToken, String name, Set<String> selectedDays) {
        return given()
            .cookie("access_token", accessToken)
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "selectedDays", selectedDays))
        .when()
            .post("/api/routine");
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

    private void rejectActivityUpdate(String accessToken, Long routineId, String date, boolean completed) {
        given()
            .cookie("access_token", accessToken)
            .contentType(ContentType.JSON)
            .body(Map.of("date", date, "completed", completed))
        .when()
            .put("/api/routine/" + routineId + "/activity")
        .then()
            .statusCode(400);
    }

    private LocalDate nextDateForDifferentDay(LocalDate date, DayOfWeek scheduledDay) {
        LocalDate candidate = date;
        do {
            candidate = candidate.plusDays(1);
        } while (candidate.getDayOfWeek() == scheduledDay);
        return candidate;
    }

    private String uniqueUsername() {
        return "t" + UUID.randomUUID().toString().substring(0, 19);
    }
}