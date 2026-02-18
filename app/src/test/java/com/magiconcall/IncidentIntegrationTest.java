package com.magiconcall;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class IncidentIntegrationTest {

    private static final String API_KEY = "test-api-key";
    private static final String TENANT = "tenant-acme";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("POST /api/v1/incidents — create incident, verify NEW status + timeline")
    void createIncident() {
        String body = """
            {
                "externalId": "jira-INC-001",
                "title": "Production database connection pool exhausted",
                "summary": "All prod DB connections saturated, causing 503s",
                "severity": "SEV1",
                "commanderName": "alice@acme.com",
                "tags": {"team": "platform", "service": "api-gateway"}
            }
            """;

        String incidentId = given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body(body)
        .when()
            .post("/api/v1/incidents")
        .then()
            .statusCode(201)
            .body("externalId", equalTo("jira-INC-001"))
            .body("severity", equalTo("SEV1"))
            .body("status", equalTo("NEW"))
            .body("commanderName", equalTo("alice@acme.com"))
            .body("id", notNullValue())
            .extract().jsonPath().getString("id");

        // Verify timeline has CREATED event
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/timeline", incidentId)
        .then()
            .statusCode(200)
            .body("$.size()", equalTo(1))
            .body("[0].eventType", equalTo("CREATED"))
            .body("[0].description", containsString("Incident created"));
    }

    @Test
    @DisplayName("POST /api/v1/incidents — idempotent on same externalId")
    void createIncident_idempotent() {
        String body = """
            {
                "externalId": "idem-001",
                "title": "Idempotent test",
                "severity": "SEV3"
            }
            """;

        String id1 = given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body(body)
        .when()
            .post("/api/v1/incidents")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");

        // Second call returns same incident (idempotent)
        String id2 = given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body(body)
        .when()
            .post("/api/v1/incidents")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");

        org.assertj.core.api.Assertions.assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("GET /api/v1/incidents/{id} — retrieve incident by ID")
    void getIncident() {
        String id = createTestIncident("get-test-001", "SEV2");

        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}", id)
        .then()
            .statusCode(200)
            .body("severity", equalTo("SEV2"))
            .body("status", equalTo("NEW"));
    }

    @Test
    @DisplayName("POST /api/v1/incidents/{id}/transition — walk the full state machine")
    void transitionStateMachine() {
        String id = createTestIncident("sm-test-001", "SEV1");

        transition(id, "TRIAGING", 200, "TRIAGING");
        transition(id, "INVESTIGATING", 200, "INVESTIGATING");
        transition(id, "MITIGATING", 200, "MITIGATING");
        transition(id, "MONITORING", 200, "MONITORING");
        transition(id, "RESOLVED", 200, "RESOLVED");
        transition(id, "POSTMORTEM", 200, "POSTMORTEM");

        // Verify timeline has all status change events
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/timeline", id)
        .then()
            .statusCode(200)
            .body("$.size()", equalTo(7)); // 1 CREATED + 6 STATUS_CHANGED
    }

    @Test
    @DisplayName("POST /api/v1/incidents/{id}/transition — invalid transition returns 409")
    void invalidTransition() {
        String id = createTestIncident("invalid-sm-001", "SEV2");

        // NEW → RESOLVED is not valid
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .post("/api/v1/incidents/{id}/transition?status=RESOLVED", id)
        .then()
            .statusCode(409)
            .body("error", equalTo("INVALID_TRANSITION"));
    }

    @Test
    @DisplayName("POST /api/v1/incidents/{id}/hypotheses — add hypothesis to incident")
    void addHypothesis() {
        String id = createTestIncident("hyp-test-001", "SEV2");

        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "title": "Connection pool leak in ORM layer",
                    "description": "Hibernate sessions not being closed after timeout",
                    "confidence": 0.7,
                    "source": "HUMAN"
                }
                """)
        .when()
            .post("/api/v1/incidents/{id}/hypotheses", id)
        .then()
            .statusCode(201)
            .body("title", equalTo("Connection pool leak in ORM layer"))
            .body("status", equalTo("PROPOSED"))
            .body("confidence", equalTo(0.7f))
            .body("source", equalTo("HUMAN"))
            .body("incidentId", equalTo(id));

        // Verify timeline includes HYPOTHESIS_ADDED
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/timeline", id)
        .then()
            .body("$.size()", equalTo(2))
            .body("[1].eventType", equalTo("HYPOTHESIS_ADDED"));
    }

    @Test
    @DisplayName("POST /api/v1/incidents/{id}/evidence — add evidence to incident")
    void addEvidence() {
        String id = createTestIncident("ev-test-001", "SEV2");

        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "evidenceType": "LOG",
                    "title": "HikariCP connection timeout stacktrace",
                    "content": "java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available",
                    "sourceUrl": "https://grafana.acme.com/logs/12345"
                }
                """)
        .when()
            .post("/api/v1/incidents/{id}/evidence", id)
        .then()
            .statusCode(201)
            .body("evidenceType", equalTo("LOG"))
            .body("title", equalTo("HikariCP connection timeout stacktrace"))
            .body("incidentId", equalTo(id));

        // Verify timeline includes EVIDENCE_ADDED
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/timeline", id)
        .then()
            .body("$.size()", equalTo(2))
            .body("[1].eventType", equalTo("EVIDENCE_ADDED"));
    }

    @Test
    @DisplayName("GET /api/v1/incidents/{id} — 404 for non-existent")
    void getIncident_notFound() {
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}", "00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(404);
    }

    // ── Helpers ─────────────────────────────────────────

    private String createTestIncident(String externalId, String severity) {
        return given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "externalId": "%s",
                    "title": "Test incident %s",
                    "severity": "%s"
                }
                """.formatted(externalId, externalId, severity))
        .when()
            .post("/api/v1/incidents")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");
    }

    private void transition(String id, String status, int expectedCode, String expectedStatus) {
        var spec = given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .post("/api/v1/incidents/{id}/transition?status={status}", id, status)
        .then()
            .statusCode(expectedCode);

        if (expectedStatus != null) {
            spec.body("status", equalTo(expectedStatus));
        }
    }
}
