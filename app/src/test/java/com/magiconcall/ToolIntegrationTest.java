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
class ToolIntegrationTest {

    private static final String API_KEY = "test-api-key";
    private static final String TENANT = "tenant-acme";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("POST /api/v1/tools/logs/run — execute logs tool successfully")
    void executeLogsTool() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "parameters": {"service": "api-gateway", "timeRange": "2h"},
                    "requestedBy": "alice@acme.com"
                }
                """)
        .when()
            .post("/api/v1/tools/{toolName}/run", "logs")
        .then()
            .statusCode(200)
            .body("toolName", equalTo("logs"))
            .body("status", equalTo("SUCCESS"))
            .body("content", containsString("api-gateway"))
            .body("executionId", notNullValue())
            .body("durationMs", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("POST /api/v1/tools/metrics/run — execute metrics tool")
    void executeMetricsTool() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "parameters": {"service": "order-service", "metric": "error_rate"},
                    "requestedBy": "bob@acme.com"
                }
                """)
        .when()
            .post("/api/v1/tools/{toolName}/run", "metrics")
        .then()
            .statusCode(200)
            .body("toolName", equalTo("metrics"))
            .body("status", equalTo("SUCCESS"))
            .body("content", containsString("order-service"));
    }

    @Test
    @DisplayName("POST /api/v1/tools/unknown/run — 404 for unknown tool")
    void unknownTool() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "parameters": {},
                    "requestedBy": "tester"
                }
                """)
        .when()
            .post("/api/v1/tools/{toolName}/run", "nonexistent")
        .then()
            .statusCode(404)
            .body("error", equalTo("TOOL_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/tools/logs/run with incidentId — stores Evidence + timeline")
    void executeToolWithIncidentId() {
        // First create an incident
        String incidentId = given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "externalId": "tool-test-inc-001",
                    "title": "Tool test incident",
                    "severity": "SEV2"
                }
                """)
        .when()
            .post("/api/v1/incidents")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");

        // Run tool linked to incident
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "incidentId": "%s",
                    "parameters": {"service": "api-gateway"},
                    "requestedBy": "alice@acme.com"
                }
                """.formatted(incidentId))
        .when()
            .post("/api/v1/tools/{toolName}/run", "logs")
        .then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"))
            .body("evidenceId", notNullValue());

        // Verify timeline shows EVIDENCE_ADDED
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/timeline", incidentId)
        .then()
            .statusCode(200)
            .body("$.size()", equalTo(2)) // CREATED + EVIDENCE_ADDED
            .body("[1].eventType", equalTo("EVIDENCE_ADDED"));
    }

    @Test
    @DisplayName("GET /api/v1/tools — lists available tools")
    void listTools() {
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/tools")
        .then()
            .statusCode(200)
            .body("$", hasSize(4))
            .body("$", hasItems("logs", "metrics", "deploy", "topology"));
    }
}
