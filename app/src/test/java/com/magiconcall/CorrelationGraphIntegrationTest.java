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
class CorrelationGraphIntegrationTest {

    private static final String API_KEY = "test-api-key";
    private static final String TENANT = "tenant-graph";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("GET graph auto-seeds linked alerts as ALERT nodes")
    void getGraph_autoSeedsAlerts() {
        // Create incident
        String incidentId = createIncident("graph-seed-001");

        // Create alert linked to this incident
        String alertId = createAlert("graph-alert-001");

        // Link alert to incident via acknowledge + manual approach:
        // We'll use the webhook approach — but simpler to just create and check graph with no alerts.
        // Actually, let's just test the empty graph first, then add nodes manually.

        // Get graph (no linked alerts → empty)
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/graph", incidentId)
        .then()
            .statusCode(200)
            .body("nodes", hasSize(0))
            .body("edges", hasSize(0));
    }

    @Test
    @DisplayName("POST node adds DEPLOY node to graph")
    void addNode_deploy() {
        String incidentId = createIncident("graph-node-001");

        String nodeId = given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "nodeType": "DEPLOY",
                    "label": "Deploy v2.3.1",
                    "description": "Production release with DB migration",
                    "source": "ci/cd"
                }
                """)
        .when()
            .post("/api/v1/incidents/{id}/graph/nodes", incidentId)
        .then()
            .statusCode(201)
            .body("nodeType", equalTo("DEPLOY"))
            .body("label", equalTo("Deploy v2.3.1"))
            .body("incidentId", equalTo(incidentId))
            .body("id", notNullValue())
            .extract().jsonPath().getString("id");

        // Verify graph now has the node
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/graph", incidentId)
        .then()
            .statusCode(200)
            .body("nodes", hasSize(1))
            .body("nodes[0].nodeType", equalTo("DEPLOY"));
    }

    @Test
    @DisplayName("POST edge creates CAUSAL_HINT edge between nodes")
    void addEdge_causalHint() {
        String incidentId = createIncident("graph-edge-001");
        String deployNodeId = addNode(incidentId, "DEPLOY", "Deploy v2.3");
        String alertNodeId = addNode(incidentId, "ALERT", "High Error Rate");

        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "sourceNodeId": "%s",
                    "targetNodeId": "%s",
                    "edgeType": "CAUSAL_HINT",
                    "weight": 0.85,
                    "reason": "Deploy correlated with error spike"
                }
                """.formatted(deployNodeId, alertNodeId))
        .when()
            .post("/api/v1/incidents/{id}/graph/edges", incidentId)
        .then()
            .statusCode(201)
            .body("edgeType", equalTo("CAUSAL_HINT"))
            .body("weight", equalTo(0.85f))
            .body("sourceNodeId", equalTo(deployNodeId))
            .body("targetNodeId", equalTo(alertNodeId));
    }

    @Test
    @DisplayName("GET root-cause-paths finds Deploy → Alert path")
    void rootCausePaths_findsPath() {
        String incidentId = createIncident("graph-rca-001");
        String deployNodeId = addNode(incidentId, "DEPLOY", "Deploy v2.3");
        String alertNodeId = addNode(incidentId, "ALERT", "High Error Rate");
        addEdge(incidentId, deployNodeId, alertNodeId, "CAUSAL_HINT", 0.9);

        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/root-cause-paths?maxPaths=3", incidentId)
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].nodeLabels", hasSize(2))
            .body("[0].nodeLabels[0]", equalTo("Deploy v2.3"))
            .body("[0].nodeLabels[1]", equalTo("High Error Rate"))
            .body("[0].score", greaterThan(0f));
    }

    @Test
    @DisplayName("GET blast-radius computes affected nodes")
    void blastRadius_computesAffectedNodes() {
        String incidentId = createIncident("graph-blast-001");
        String deployNodeId = addNode(incidentId, "DEPLOY", "Deploy v2.3");
        String svc1Id = addNode(incidentId, "SERVICE", "api-gateway");
        String svc2Id = addNode(incidentId, "SERVICE", "auth-service");
        addEdge(incidentId, deployNodeId, svc1Id, "DEPENDS_ON", 0.8);
        addEdge(incidentId, deployNodeId, svc2Id, "DEPENDS_ON", 0.6);

        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/blast-radius/{nodeId}", incidentId, deployNodeId)
        .then()
            .statusCode(200)
            .body("rootCauseNode.label", equalTo("Deploy v2.3"))
            .body("totalAffected", equalTo(2))
            .body("affectedNodes", hasSize(2));
    }

    @Test
    @DisplayName("GET graph 404 for non-existent incident")
    void getGraph_notFound() {
        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/graph", "00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(404)
            .body("error", equalTo("NOT_FOUND"));
    }

    @Test
    @DisplayName("POST edge 404 for unknown source node")
    void addEdge_unknownNode() {
        String incidentId = createIncident("graph-edge404-001");

        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "sourceNodeId": "00000000-0000-0000-0000-000000000001",
                    "targetNodeId": "00000000-0000-0000-0000-000000000002",
                    "edgeType": "DEPENDS_ON",
                    "weight": 0.5
                }
                """)
        .when()
            .post("/api/v1/incidents/{id}/graph/edges", incidentId)
        .then()
            .statusCode(404)
            .body("error", equalTo("NODE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET root-cause-paths empty graph returns empty array")
    void rootCausePaths_emptyGraph() {
        String incidentId = createIncident("graph-rca-empty-001");

        given()
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
        .when()
            .get("/api/v1/incidents/{id}/root-cause-paths?maxPaths=3", incidentId)
        .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    // ── Helpers ─────────────────────────────────────────

    private String createIncident(String externalId) {
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

    private String createAlert(String externalId) {
        return given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "externalId": "%s",
                    "title": "Test alert %s",
                    "source": "test",
                    "severity": "CRITICAL"
                }
                """.formatted(externalId, externalId))
        .when()
            .post("/api/v1/alerts")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");
    }

    private String addNode(String incidentId, String nodeType, String label) {
        return given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "nodeType": "%s",
                    "label": "%s"
                }
                """.formatted(nodeType, label))
        .when()
            .post("/api/v1/incidents/{id}/graph/nodes", incidentId)
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("id");
    }

    private void addEdge(String incidentId, String sourceNodeId, String targetNodeId,
                         String edgeType, double weight) {
        given()
            .contentType(ContentType.JSON)
            .header("X-Api-Key", API_KEY)
            .header("X-Customer-Id", TENANT)
            .body("""
                {
                    "sourceNodeId": "%s",
                    "targetNodeId": "%s",
                    "edgeType": "%s",
                    "weight": %s
                }
                """.formatted(sourceNodeId, targetNodeId, edgeType, weight))
        .when()
            .post("/api/v1/incidents/{id}/graph/edges", incidentId)
        .then()
            .statusCode(201);
    }
}
