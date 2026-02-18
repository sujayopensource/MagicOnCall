package com.magiconcall;

import com.magiconcall.infrastructure.persistence.outbox.OutboxRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class AlertIntegrationTest {

    private static final String API_KEY = "test-api-key";
    private static final String TENANT_ID = "tenant-acme";

    @LocalServerPort
    private int port;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("POST /api/v1/alerts — ingest alert with auth + tenant headers")
    void ingestAlert_success() {
        String body = """
            {
                "externalId": "pagerduty-12345",
                "title": "CPU usage > 95% on prod-web-01",
                "description": "Sustained high CPU for 10 minutes",
                "source": "pagerduty",
                "severity": "CRITICAL",
                "labels": {"team": "platform", "env": "production"}
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
            .body(body)
        .when()
            .post("/api/v1/alerts")
        .then()
            .statusCode(201)
            .body("externalId", equalTo("pagerduty-12345"))
            .body("severity", equalTo("CRITICAL"))
            .body("status", equalTo("OPEN"))
            .body("policyDecisionReason", containsString("auto-escalation"))
            .body("id", notNullValue());

        // Verify outbox event was created
        var outboxEvents = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        var alertCreatedEvents = outboxEvents.stream()
            .filter(e -> "ALERT_CREATED".equals(e.getEventType()))
            .toList();
        assertThat(alertCreatedEvents).isNotEmpty();
        assertThat(alertCreatedEvents.get(0).getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("POST /api/v1/alerts — missing X-Api-Key returns 401")
    void ingestAlert_missingApiKey() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Customer-Id", TENANT_ID)
            .body("{\"externalId\":\"x\",\"title\":\"t\",\"source\":\"s\",\"severity\":\"INFO\"}")
        .when()
            .post("/api/v1/alerts")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("POST /api/v1/alerts — missing X-Customer-Id returns 400")
    void ingestAlert_missingTenant() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .body("{\"externalId\":\"x\",\"title\":\"t\",\"source\":\"s\",\"severity\":\"INFO\"}")
        .when()
            .post("/api/v1/alerts")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/v1/alerts — duplicate externalId is rejected by policy")
    void ingestAlert_duplicate_denied() {
        String body = """
            {
                "externalId": "dup-001",
                "title": "Disk full on db-01",
                "source": "datadog",
                "severity": "HIGH"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
            .body(body)
        .when()
            .post("/api/v1/alerts")
        .then()
            .statusCode(201);

        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
            .body(body)
        .when()
            .post("/api/v1/alerts")
        .then()
            .statusCode(409)
            .body("message", containsString("Duplicate"));
    }

    @Test
    @DisplayName("GET /api/v1/alerts/{id} — retrieve an alert by ID")
    void getAlert_success() {
        String body = """
            {
                "externalId": "get-test-001",
                "title": "Memory leak in auth-service",
                "source": "grafana",
                "severity": "WARNING"
            }
            """;

        String alertId = given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
            .body(body)
        .when()
            .post("/api/v1/alerts")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");

        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
        .when()
            .get("/api/v1/alerts/{id}", alertId)
        .then()
            .statusCode(200)
            .body("title", equalTo("Memory leak in auth-service"))
            .body("severity", equalTo("WARNING"));
    }

    @Test
    @DisplayName("POST /api/v1/alerts/{id}/acknowledge — transition to ACKNOWLEDGED")
    void acknowledgeAlert() {
        String body = """
            {
                "externalId": "ack-test-001",
                "title": "Latency spike on api-gateway",
                "source": "newrelic",
                "severity": "HIGH"
            }
            """;

        String alertId = given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
            .body(body)
        .when()
            .post("/api/v1/alerts")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");

        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
        .when()
            .post("/api/v1/alerts/{id}/acknowledge", alertId)
        .then()
            .statusCode(200)
            .body("status", equalTo("ACKNOWLEDGED"));
    }

    @Test
    @DisplayName("POST /api/v1/alerts/{id}/resolve — transition to RESOLVED")
    void resolveAlert() {
        String body = """
            {
                "externalId": "resolve-test-001",
                "title": "SSL cert expiring",
                "source": "certmonitor",
                "severity": "INFO"
            }
            """;

        String alertId = given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
            .body(body)
        .when()
            .post("/api/v1/alerts")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");

        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
        .when()
            .post("/api/v1/alerts/{id}/resolve", alertId)
        .then()
            .statusCode(200)
            .body("status", equalTo("RESOLVED"));
    }

    @Test
    @DisplayName("GET /api/v1/alerts?status=OPEN — list open alerts")
    void listOpenAlerts() {
        for (int i = 1; i <= 2; i++) {
            given()
                .contentType(ContentType.JSON)
                .header("X-Api-Key", API_KEY)
                .header("X-Customer-Id", TENANT_ID)
                .body("""
                    {
                        "externalId": "list-test-%d",
                        "title": "Alert %d",
                        "source": "test",
                        "severity": "WARNING"
                    }
                    """.formatted(i, i))
            .when()
                .post("/api/v1/alerts")
            .then()
                .statusCode(201);
        }

        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
        .when()
            .get("/api/v1/alerts?status=OPEN")
        .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(2));
    }

    @Test
    @DisplayName("GET /api/v1/alerts/{id} — 404 for non-existent alert")
    void getAlert_notFound() {
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT_ID)
        .when()
            .get("/api/v1/alerts/{id}", "00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(404);
    }
}
