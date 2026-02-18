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
class PagerDutyWebhookIntegrationTest {

    private static final String TENANT = "tenant-acme";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("POST /webhooks/{tenantId}/pagerduty — ingest alert + auto-create incident")
    void ingestNewAlert() {
        String payload = pdPayload("PD-INC-001", "SVC-A", "Database connection pool exhausted",
            "high", "dedup-key-001");

        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/webhooks/{tenantId}/pagerduty", TENANT)
        .then()
            .statusCode(202)
            .body("status", equalTo("accepted"))
            .body("processed", equalTo(1))
            .body("results[0].alertId", notNullValue())
            .body("results[0].incidentId", notNullValue())
            .body("results[0].newIncident", equalTo(true))
            .body("results[0].deduplicated", equalTo(false));
    }

    @Test
    @DisplayName("POST /webhooks/{tenantId}/pagerduty — dedup returns existing alert")
    void deduplicateAlert() {
        String payload = pdPayload("PD-INC-002", "SVC-B", "CPU spike alert",
            "high", "dedup-key-002");

        // First call — creates
        String alertId1 = given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/webhooks/{tenantId}/pagerduty", TENANT)
        .then()
            .statusCode(202)
            .extract().jsonPath().getString("results[0].alertId");

        // Second call — deduplicated
        String alertId2 = given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/webhooks/{tenantId}/pagerduty", TENANT)
        .then()
            .statusCode(202)
            .body("results[0].deduplicated", equalTo(true))
            .extract().jsonPath().getString("results[0].alertId");

        org.assertj.core.api.Assertions.assertThat(alertId1).isEqualTo(alertId2);
    }

    @Test
    @DisplayName("POST /webhooks/{tenantId}/pagerduty — second alert attaches to existing incident")
    void attachToExistingIncident() {
        // First alert creates incident
        String payload1 = pdPayload("PD-INC-003", "SVC-C", "First alert",
            "high", "dedup-key-003a");

        String incidentId1 = given()
            .contentType(ContentType.JSON)
            .body(payload1)
        .when()
            .post("/webhooks/{tenantId}/pagerduty", TENANT)
        .then()
            .statusCode(202)
            .body("results[0].newIncident", equalTo(true))
            .extract().jsonPath().getString("results[0].incidentId");

        // Second alert for SAME PD incident (different dedup key) attaches to existing
        String payload2 = pdPayload("PD-INC-003", "SVC-C", "Second alert on same incident",
            "high", "dedup-key-003b");

        String incidentId2 = given()
            .contentType(ContentType.JSON)
            .body(payload2)
        .when()
            .post("/webhooks/{tenantId}/pagerduty", TENANT)
        .then()
            .statusCode(202)
            .body("results[0].newIncident", equalTo(false))
            .extract().jsonPath().getString("results[0].incidentId");

        org.assertj.core.api.Assertions.assertThat(incidentId1).isEqualTo(incidentId2);
    }

    @Test
    @DisplayName("POST /webhooks/{tenantId}/pagerduty — incident timeline shows correlated alerts")
    void timelineShowsCorrelatedAlerts() {
        String payload = pdPayload("PD-INC-004", "SVC-D", "Timeline test alert",
            "low", "dedup-key-004");

        String incidentId = given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/webhooks/{tenantId}/pagerduty", TENANT)
        .then()
            .statusCode(202)
            .extract().jsonPath().getString("results[0].incidentId");

        // Verify timeline via API (requires auth headers)
        given()
            .header("X-Api-Key", "test-api-key")
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/timeline", incidentId)
        .then()
            .statusCode(200)
            .body("$.size()", equalTo(2)) // CREATED + ALERT_CORRELATED
            .body("[0].eventType", equalTo("CREATED"))
            .body("[1].eventType", equalTo("ALERT_CORRELATED"));
    }

    @Test
    @DisplayName("POST /webhooks/{tenantId}/pagerduty — low urgency maps to SEV3")
    void lowUrgencyMapsSev3() {
        String payload = pdPayload("PD-INC-005", "SVC-E", "Low urgency test",
            "low", "dedup-key-005");

        String incidentId = given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/webhooks/{tenantId}/pagerduty", TENANT)
        .then()
            .statusCode(202)
            .extract().jsonPath().getString("results[0].incidentId");

        // Verify incident has SEV3
        given()
            .header("X-Api-Key", "test-api-key")
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}", incidentId)
        .then()
            .statusCode(200)
            .body("severity", equalTo("SEV3"));
    }

    @Test
    @DisplayName("POST /webhooks/{tenantId}/pagerduty — empty messages returns 400")
    void emptyPayload() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"messages\": []}")
        .when()
            .post("/webhooks/{tenantId}/pagerduty", TENANT)
        .then()
            .statusCode(400)
            .body("error", equalTo("INVALID_PAYLOAD"));
    }

    @Test
    @DisplayName("POST /webhooks/{tenantId}/pagerduty — bypasses API key auth")
    void bypassesApiKeyAuth() {
        // No X-Api-Key header — should still work (webhooks bypass /api/** auth)
        String payload = pdPayload("PD-INC-006", "SVC-F", "No auth needed",
            "high", "dedup-key-006");

        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/webhooks/{tenantId}/pagerduty", TENANT)
        .then()
            .statusCode(202);
    }

    // ── Helper ─────────────────────────────────────────

    private String pdPayload(String incidentId, String serviceId, String title,
                              String urgency, String incidentKey) {
        return """
            {
                "messages": [
                    {
                        "event": "incident.trigger",
                        "incident": {
                            "id": "%s",
                            "incident_number": 1,
                            "title": "%s",
                            "description": "Auto-generated test incident",
                            "status": "triggered",
                            "urgency": "%s",
                            "html_url": "https://acme.pagerduty.com/incidents/%s",
                            "service": {
                                "id": "%s",
                                "name": "test-service",
                                "description": "Test service"
                            },
                            "alert_counts": {
                                "triggered": 1,
                                "acknowledged": 0,
                                "resolved": 0
                            },
                            "incident_key": "%s"
                        }
                    }
                ]
            }
            """.formatted(incidentId, title, urgency, incidentId, serviceId, incidentKey);
    }
}
