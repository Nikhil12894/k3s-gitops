package com.explorewithnk.appcore;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class PublicControllerTest {

    @Test
    public void testPingEndpoint() {
        given()
                .when().get("/api/public/ping")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    public void testActuatorHealth() {
        given()
                .when().get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }
}
