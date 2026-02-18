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
class TriageIntegrationTest {

    private static final String API_KEY = "test-api-key";
    private static final String TENANT = "tenant-triage";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("POST /api/v1/incidents/{id}/triage — generates hypotheses with MockLlmClient")
    void triageGeneratesHypotheses() {
        String incidentId = createTestIncident("triage-001");

        // Add some evidence first
        addEvidence(incidentId, "LOG", "Connection timeout stacktrace",
            "java.sql.SQLTransientConnectionException");
        addEvidence(incidentId, "METRIC", "CPU usage spike",
            "CPU at 95% for the last 10 minutes");

        // Trigger triage
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .post("/api/v1/incidents/{id}/triage", incidentId)
        .then()
            .statusCode(200)
            .body("incidentId", equalTo(incidentId))
            .body("hypotheses.size()", greaterThan(0))
            .body("hypotheses[0].source", equalTo("AI"))
            .body("hypotheses[0].evidenceFor", notNullValue())
            .body("hypotheses[0].nextBestTest", notNullValue())
            .body("evidenceHash", notNullValue())
            .body("cached", equalTo(false));
    }

    @Test
    @DisplayName("POST /api/v1/incidents/{id}/triage — second call returns cached result")
    void triageCacheHit() {
        String incidentId = createTestIncident("triage-cache-001");

        addEvidence(incidentId, "LOG", "Error log", "Some error content");

        // First triage
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .post("/api/v1/incidents/{id}/triage", incidentId)
        .then()
            .statusCode(200)
            .body("cached", equalTo(false));

        // Second triage with same evidence → cache hit
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .post("/api/v1/incidents/{id}/triage", incidentId)
        .then()
            .statusCode(200)
            .body("cached", equalTo(true))
            .body("tokensUsed", equalTo(0));
    }

    @Test
    @DisplayName("GET /api/v1/incidents/{id}/hypotheses — returns hypotheses list")
    void getHypotheses() {
        String incidentId = createTestIncident("triage-get-hyp-001");

        // Add a manual hypothesis
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "title": "Manual hypothesis",
                    "description": "Manually created",
                    "confidence": 0.6,
                    "source": "HUMAN"
                }
                """)
        .when()
            .post("/api/v1/incidents/{id}/hypotheses", incidentId)
        .then()
            .statusCode(201);

        // GET hypotheses
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/hypotheses", incidentId)
        .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1))
            .body("[0].title", equalTo("Manual hypothesis"))
            .body("[0].source", equalTo("HUMAN"));
    }

    @Test
    @DisplayName("POST /api/v1/incidents/{id}/triage — 404 for non-existent incident")
    void triageIncidentNotFound() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .post("/api/v1/incidents/{id}/triage", "00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(404)
            .body("error", equalTo("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/incidents/{id}/hypotheses — empty list for incident with no hypotheses")
    void getHypothesesEmpty() {
        String incidentId = createTestIncident("triage-empty-hyp-001");

        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/hypotheses", incidentId)
        .then()
            .statusCode(200)
            .body("$.size()", equalTo(0));
    }

    // ── Helpers ─────────────────────────────────────────

    private String createTestIncident(String externalId) {
        return given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "externalId": "%s",
                    "title": "Test incident %s",
                    "severity": "SEV2"
                }
                """.formatted(externalId, externalId))
        .when()
            .post("/api/v1/incidents")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");
    }

    private void addEvidence(String incidentId, String type, String title, String content) {
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "evidenceType": "%s",
                    "title": "%s",
                    "content": "%s"
                }
                """.formatted(type, title, content))
        .when()
            .post("/api/v1/incidents/{id}/evidence", incidentId)
        .then()
            .statusCode(201);
    }
}
