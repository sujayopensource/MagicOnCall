package com.magiconcall.application.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.application.incident.IncidentService;
import com.magiconcall.domain.alert.Alert;
import com.magiconcall.domain.alert.AlertRepository;
import com.magiconcall.domain.alert.AlertSeverity;
import com.magiconcall.domain.graph.*;
import com.magiconcall.domain.incident.*;
import com.magiconcall.domain.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationGraphServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private CorrelationNodeRepository nodeRepository;
    @Mock private CorrelationEdgeRepository edgeRepository;
    @Mock private IncidentEventRepository incidentEventRepository;

    private CorrelationGraphService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID INCIDENT_ID = UUID.randomUUID();
    private static final String TENANT = "tenant-test";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT);
        service = new CorrelationGraphService(
            incidentRepository, alertRepository, nodeRepository,
            edgeRepository, incidentEventRepository, objectMapper
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("getGraph auto-seeds alert nodes from linked alerts")
    void getGraph_autoSeedsAlertNodes() {
        var incident = createIncident();
        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));

        var alert = new Alert("ext-1", "High CPU Alert", "CPU > 90%", "pagerduty",
            AlertSeverity.CRITICAL, "{}", "default_allow_rule");
        alert.setId(UUID.randomUUID());
        alert.setIncidentId(INCIDENT_ID);
        when(alertRepository.findByIncidentId(INCIDENT_ID)).thenReturn(List.of(alert));
        when(nodeRepository.findByIncidentIdAndReferenceId(INCIDENT_ID, alert.getId()))
            .thenReturn(List.of());
        when(nodeRepository.save(any(CorrelationNode.class))).thenAnswer(inv -> {
            CorrelationNode n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(incidentEventRepository.save(any(IncidentEvent.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(nodeRepository.findByIncidentId(INCIDENT_ID)).thenReturn(List.of());
        when(edgeRepository.findByIncidentId(INCIDENT_ID)).thenReturn(List.of());

        var result = service.getGraph(INCIDENT_ID);

        verify(nodeRepository).save(any(CorrelationNode.class));
        verify(incidentEventRepository).save(any(IncidentEvent.class));
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getGraph idempotent seeding — skips already-seeded alerts")
    void getGraph_idempotentSeeding() {
        var incident = createIncident();
        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));

        var alert = new Alert("ext-1", "Alert", "desc", "pd", AlertSeverity.WARNING, "{}", "rule");
        alert.setId(UUID.randomUUID());
        when(alertRepository.findByIncidentId(INCIDENT_ID)).thenReturn(List.of(alert));

        // Already seeded
        var existingNode = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.ALERT,
            "Alert", "desc", alert.getId(), "pd", "{}");
        when(nodeRepository.findByIncidentIdAndReferenceId(INCIDENT_ID, alert.getId()))
            .thenReturn(List.of(existingNode));
        when(nodeRepository.findByIncidentId(INCIDENT_ID)).thenReturn(List.of(existingNode));
        when(edgeRepository.findByIncidentId(INCIDENT_ID)).thenReturn(List.of());

        service.getGraph(INCIDENT_ID);

        verify(nodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("addNode creates node and timeline event")
    void addNode_createsNodeAndTimeline() {
        var incident = createIncident();
        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
        when(nodeRepository.save(any(CorrelationNode.class))).thenAnswer(inv -> {
            CorrelationNode n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });
        when(incidentEventRepository.save(any(IncidentEvent.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        var command = new AddNodeCommand("DEPLOY", "Deploy v2.3.1",
            "Production release", null, "ci/cd", null);
        var result = service.addNode(INCIDENT_ID, command);

        assertThat(result.nodeType()).isEqualTo("DEPLOY");
        assertThat(result.label()).isEqualTo("Deploy v2.3.1");
        verify(nodeRepository).save(any(CorrelationNode.class));
        verify(incidentEventRepository).save(any(IncidentEvent.class));
    }

    @Test
    @DisplayName("addEdge validates nodes belong to incident")
    void addEdge_validatesNodesBelongToIncident() {
        var incident = createIncident();
        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));

        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        var sourceNode = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.DEPLOY,
            "Deploy", null, null, null, "{}");
        sourceNode.setId(sourceId);
        var targetNode = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.ALERT,
            "Alert", null, null, null, "{}");
        targetNode.setId(targetId);

        when(nodeRepository.findById(sourceId)).thenReturn(Optional.of(sourceNode));
        when(nodeRepository.findById(targetId)).thenReturn(Optional.of(targetNode));
        when(edgeRepository.save(any(CorrelationEdge.class))).thenAnswer(inv -> {
            CorrelationEdge e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        var command = new AddEdgeCommand(sourceId, targetId, "CAUSAL_HINT", 0.8, "Deploy caused alert", null);
        var result = service.addEdge(INCIDENT_ID, command);

        assertThat(result.edgeType()).isEqualTo("CAUSAL_HINT");
        assertThat(result.weight()).isEqualTo(0.8);
        verify(edgeRepository).save(any(CorrelationEdge.class));
    }

    @Test
    @DisplayName("addEdge throws NodeNotFoundException for unknown node")
    void addEdge_throwsNodeNotFound() {
        var incident = createIncident();
        when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));

        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(nodeRepository.findById(sourceId)).thenReturn(Optional.empty());

        var command = new AddEdgeCommand(sourceId, targetId, "DEPENDS_ON", 0.5, null, null);

        assertThatThrownBy(() -> service.addEdge(INCIDENT_ID, command))
            .isInstanceOf(CorrelationGraphService.NodeNotFoundException.class);
    }

    @Test
    @DisplayName("findRootCausePaths returns empty for graph with no edges")
    void findRootCausePaths_emptyGraph() {
        when(incidentRepository.findById(INCIDENT_ID))
            .thenReturn(Optional.of(createIncident()));
        when(nodeRepository.findByIncidentId(INCIDENT_ID)).thenReturn(List.of());
        when(edgeRepository.findByIncidentId(INCIDENT_ID)).thenReturn(List.of());

        var paths = service.findRootCausePaths(INCIDENT_ID, 3);

        assertThat(paths).isEmpty();
    }

    @Test
    @DisplayName("findRootCausePaths finds simple chain Deploy → Service → Alert")
    void findRootCausePaths_simpleChain() {
        when(incidentRepository.findById(INCIDENT_ID))
            .thenReturn(Optional.of(createIncident()));

        UUID deployId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();

        var deployNode = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.DEPLOY,
            "Deploy v2.3", null, null, null, "{}");
        deployNode.setId(deployId);
        var serviceNode = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.SERVICE,
            "api-gateway", null, null, null, "{}");
        serviceNode.setId(serviceId);
        var alertNode = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.ALERT,
            "High Error Rate", null, null, null, "{}");
        alertNode.setId(alertId);

        var edge1 = new CorrelationEdge(INCIDENT_ID, deployId, serviceId,
            CorrelationEdgeType.CAUSAL_HINT, 0.9, "Deploy triggered service issue", "{}");
        edge1.setId(UUID.randomUUID());
        var edge2 = new CorrelationEdge(INCIDENT_ID, serviceId, alertId,
            CorrelationEdgeType.DEPENDS_ON, 0.8, "Service emits alert", "{}");
        edge2.setId(UUID.randomUUID());

        when(nodeRepository.findByIncidentId(INCIDENT_ID))
            .thenReturn(List.of(deployNode, serviceNode, alertNode));
        when(edgeRepository.findByIncidentId(INCIDENT_ID))
            .thenReturn(List.of(edge1, edge2));

        var paths = service.findRootCausePaths(INCIDENT_ID, 3);

        // SERVICE is also a root cause type, so we get 2 paths:
        // 1. api-gateway → High Error Rate (shorter, higher score due to length penalty)
        // 2. Deploy v2.3 → api-gateway → High Error Rate (full chain)
        assertThat(paths).hasSize(2);
        assertThat(paths.get(0).score()).isGreaterThan(paths.get(1).score());
        // The full chain should be present
        assertThat(paths).anyMatch(p ->
            p.nodeLabels().equals(List.of("Deploy v2.3", "api-gateway", "High Error Rate")));
    }

    @Test
    @DisplayName("findRootCausePaths ranks multiple paths by score")
    void findRootCausePaths_multiplePathsRanked() {
        when(incidentRepository.findById(INCIDENT_ID))
            .thenReturn(Optional.of(createIncident()));

        UUID deploy1Id = UUID.randomUUID();
        UUID deploy2Id = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();

        var deploy1 = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.DEPLOY,
            "Deploy A", null, null, null, "{}");
        deploy1.setId(deploy1Id);
        var deploy2 = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.DEPLOY,
            "Deploy B", null, null, null, "{}");
        deploy2.setId(deploy2Id);
        var alert = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.ALERT,
            "Alert", null, null, null, "{}");
        alert.setId(alertId);

        // Path 1: Deploy A → Alert (weight 0.9)
        var edge1 = new CorrelationEdge(INCIDENT_ID, deploy1Id, alertId,
            CorrelationEdgeType.CAUSAL_HINT, 0.9, null, "{}");
        edge1.setId(UUID.randomUUID());
        // Path 2: Deploy B → Alert (weight 0.4)
        var edge2 = new CorrelationEdge(INCIDENT_ID, deploy2Id, alertId,
            CorrelationEdgeType.CAUSAL_HINT, 0.4, null, "{}");
        edge2.setId(UUID.randomUUID());

        when(nodeRepository.findByIncidentId(INCIDENT_ID))
            .thenReturn(List.of(deploy1, deploy2, alert));
        when(edgeRepository.findByIncidentId(INCIDENT_ID))
            .thenReturn(List.of(edge1, edge2));

        var paths = service.findRootCausePaths(INCIDENT_ID, 3);

        assertThat(paths).hasSize(2);
        // Higher weight path should be first
        assertThat(paths.get(0).score()).isGreaterThan(paths.get(1).score());
        assertThat(paths.get(0).nodeLabels()).contains("Deploy A");
    }

    @Test
    @DisplayName("computeBlastRadius performs BFS forward traversal")
    void computeBlastRadius_forwardTraversal() {
        when(incidentRepository.findById(INCIDENT_ID))
            .thenReturn(Optional.of(createIncident()));

        UUID deployId = UUID.randomUUID();
        UUID service1Id = UUID.randomUUID();
        UUID service2Id = UUID.randomUUID();

        var deployNode = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.DEPLOY,
            "Deploy v2.3", null, null, null, "{}");
        deployNode.setId(deployId);
        deployNode.setTenantId(TENANT);

        var service1 = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.SERVICE,
            "api-gateway", null, null, null, "{}");
        service1.setId(service1Id);
        service1.setTenantId(TENANT);

        var service2 = new CorrelationNode(INCIDENT_ID, CorrelationNodeType.SERVICE,
            "auth-service", null, null, null, "{}");
        service2.setId(service2Id);
        service2.setTenantId(TENANT);

        when(nodeRepository.findById(deployId)).thenReturn(Optional.of(deployNode));
        when(nodeRepository.findById(service1Id)).thenReturn(Optional.of(service1));
        when(nodeRepository.findById(service2Id)).thenReturn(Optional.of(service2));

        var edge1 = new CorrelationEdge(INCIDENT_ID, deployId, service1Id,
            CorrelationEdgeType.DEPENDS_ON, 0.8, null, "{}");
        var edge2 = new CorrelationEdge(INCIDENT_ID, deployId, service2Id,
            CorrelationEdgeType.DEPENDS_ON, 0.6, null, "{}");
        when(edgeRepository.findByIncidentId(INCIDENT_ID))
            .thenReturn(List.of(edge1, edge2));

        var result = service.computeBlastRadius(INCIDENT_ID, deployId);

        assertThat(result.rootCauseNode().label()).isEqualTo("Deploy v2.3");
        assertThat(result.totalAffected()).isEqualTo(2);
        assertThat(result.affectedNodes()).extracting("label")
            .containsExactlyInAnyOrder("api-gateway", "auth-service");
    }

    private Incident createIncident() {
        var incident = new Incident("ext-1", "Test Incident", "summary",
            IncidentSeverity.SEV2, null, "{}");
        incident.setTenantId(TENANT);
        return incident;
    }
}
