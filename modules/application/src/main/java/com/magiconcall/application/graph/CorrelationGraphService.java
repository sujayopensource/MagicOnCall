package com.magiconcall.application.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.application.incident.IncidentService;
import com.magiconcall.domain.alert.Alert;
import com.magiconcall.domain.alert.AlertRepository;
import com.magiconcall.domain.graph.*;
import com.magiconcall.domain.incident.IncidentEvent;
import com.magiconcall.domain.incident.IncidentEventRepository;
import com.magiconcall.domain.incident.IncidentRepository;
import com.magiconcall.domain.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CorrelationGraphService {

    private static final Logger log = LoggerFactory.getLogger(CorrelationGraphService.class);

    private static final Set<CorrelationNodeType> SYMPTOM_TYPES = Set.of(
        CorrelationNodeType.ALERT, CorrelationNodeType.METRIC_ANOMALY, CorrelationNodeType.LOG_CLUSTER
    );
    private static final Set<CorrelationNodeType> ROOT_CAUSE_TYPES = Set.of(
        CorrelationNodeType.DEPLOY, CorrelationNodeType.SERVICE
    );
    private static final Set<CorrelationEdgeType> TRAVERSABLE_EDGE_TYPES = Set.of(
        CorrelationEdgeType.CAUSAL_HINT, CorrelationEdgeType.DEPENDS_ON
    );

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final CorrelationNodeRepository nodeRepository;
    private final CorrelationEdgeRepository edgeRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final ObjectMapper objectMapper;

    public CorrelationGraphService(IncidentRepository incidentRepository,
                                   AlertRepository alertRepository,
                                   CorrelationNodeRepository nodeRepository,
                                   CorrelationEdgeRepository edgeRepository,
                                   IncidentEventRepository incidentEventRepository,
                                   ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CorrelationGraphResult getGraph(UUID incidentId) {
        var incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentService.IncidentNotFoundException(incidentId));

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            // Auto-seed ALERT nodes from linked alerts
            int seeded = seedAlertNodes(incidentId, incident.getTenantId());
            if (seeded > 0) {
                log.info("Auto-seeded {} alert nodes for incident {}", seeded, incidentId);
            }

            var nodes = nodeRepository.findByIncidentId(incidentId)
                .stream().map(GraphNodeResult::from).toList();
            var edges = edgeRepository.findByIncidentId(incidentId)
                .stream().map(GraphEdgeResult::from).toList();

            return new CorrelationGraphResult(nodes, edges);
        }
    }

    @Transactional
    public GraphNodeResult addNode(UUID incidentId, AddNodeCommand command) {
        var incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentService.IncidentNotFoundException(incidentId));

        String tenantId = incident.getTenantId();
        var nodeType = CorrelationNodeType.valueOf(command.nodeType().toUpperCase());

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            var node = new CorrelationNode(
                incidentId, nodeType, command.label(), command.description(),
                command.referenceId(), command.source(), serializeMap(command.metadata())
            );
            node.setTenantId(tenantId);
            node = nodeRepository.save(node);

            log.info("Graph node added: id={}, type={}, label={}", node.getId(), nodeType, command.label());

            var timelineEvent = IncidentEvent.graphNodeAdded(incidentId, command.label(), nodeType.name());
            timelineEvent.setTenantId(tenantId);
            incidentEventRepository.save(timelineEvent);

            return GraphNodeResult.from(node);
        }
    }

    @Transactional
    public GraphEdgeResult addEdge(UUID incidentId, AddEdgeCommand command) {
        var incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentService.IncidentNotFoundException(incidentId));

        String tenantId = incident.getTenantId();
        var edgeType = CorrelationEdgeType.valueOf(command.edgeType().toUpperCase());

        // Validate both nodes belong to this incident
        var sourceNode = nodeRepository.findById(command.sourceNodeId())
            .filter(n -> n.getIncidentId().equals(incidentId))
            .orElseThrow(() -> new NodeNotFoundException(command.sourceNodeId()));
        var targetNode = nodeRepository.findById(command.targetNodeId())
            .filter(n -> n.getIncidentId().equals(incidentId))
            .orElseThrow(() -> new NodeNotFoundException(command.targetNodeId()));

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            var edge = new CorrelationEdge(
                incidentId, command.sourceNodeId(), command.targetNodeId(),
                edgeType, command.weight(), command.reason(), serializeMap(command.metadata())
            );
            edge.setTenantId(tenantId);
            edge = edgeRepository.save(edge);

            log.info("Graph edge added: {} → {} ({})", sourceNode.getLabel(), targetNode.getLabel(), edgeType);

            return GraphEdgeResult.from(edge);
        }
    }

    @Transactional(readOnly = true)
    public List<RootCausePath> findRootCausePaths(UUID incidentId, int maxPaths) {
        incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentService.IncidentNotFoundException(incidentId));

        var nodes = nodeRepository.findByIncidentId(incidentId);
        var edges = edgeRepository.findByIncidentId(incidentId);

        if (nodes.isEmpty() || edges.isEmpty()) {
            return List.of();
        }

        // Build node map and incoming-edge adjacency map
        Map<UUID, CorrelationNode> nodeMap = new HashMap<>();
        for (var n : nodes) nodeMap.put(n.getId(), n);

        // incoming edges: targetNodeId → list of edges pointing to it
        Map<UUID, List<CorrelationEdge>> incomingEdges = new HashMap<>();
        for (var e : edges) {
            if (TRAVERSABLE_EDGE_TYPES.contains(e.getEdgeType())) {
                incomingEdges.computeIfAbsent(e.getTargetNodeId(), k -> new ArrayList<>()).add(e);
            }
        }

        // Identify symptom nodes
        List<CorrelationNode> symptoms = nodes.stream()
            .filter(n -> SYMPTOM_TYPES.contains(n.getNodeType()))
            .toList();

        // DFS backward from each symptom
        List<RootCausePath> allPaths = new ArrayList<>();
        for (var symptom : symptoms) {
            List<List<UUID>> foundPaths = new ArrayList<>();
            List<List<Double>> foundWeights = new ArrayList<>();
            dfsBackward(symptom.getId(), new ArrayList<>(List.of(symptom.getId())),
                new ArrayList<>(), new HashSet<>(), incomingEdges, nodeMap,
                foundPaths, foundWeights);

            for (int i = 0; i < foundPaths.size(); i++) {
                var pathIds = foundPaths.get(i);
                var weights = foundWeights.get(i);
                var labels = pathIds.stream().map(id -> nodeMap.get(id).getLabel()).toList();

                double weightProduct = weights.stream().reduce(1.0, (a, b) -> a * b);
                double score = weightProduct / (1.0 + weights.size() * 0.1);

                var startNode = nodeMap.get(pathIds.getFirst());
                var endNode = nodeMap.get(pathIds.getLast());
                String explanation = "%s → ... → %s (score: %.3f)".formatted(
                    endNode.getLabel(), startNode.getLabel(), score);

                // Reverse so path goes root cause → symptom
                var reversedIds = new ArrayList<>(pathIds);
                Collections.reverse(reversedIds);
                var reversedLabels = new ArrayList<>(labels);
                Collections.reverse(reversedLabels);

                allPaths.add(new RootCausePath(reversedIds, reversedLabels, score, explanation));
            }
        }

        // Sort descending by score, return top N
        allPaths.sort(Comparator.comparingDouble(RootCausePath::score).reversed());
        return allPaths.stream().limit(maxPaths).toList();
    }

    private void dfsBackward(UUID currentId, List<UUID> path, List<Double> weights,
                             Set<UUID> visited, Map<UUID, List<CorrelationEdge>> incomingEdges,
                             Map<UUID, CorrelationNode> nodeMap,
                             List<List<UUID>> foundPaths, List<List<Double>> foundWeights) {
        visited.add(currentId);

        var incoming = incomingEdges.getOrDefault(currentId, List.of());
        for (var edge : incoming) {
            UUID sourceId = edge.getSourceNodeId();
            if (visited.contains(sourceId)) continue;

            var sourceNode = nodeMap.get(sourceId);
            if (sourceNode == null) continue;

            path.add(sourceId);
            weights.add(edge.getWeight());

            // If we reached a root cause type and path has more than 1 hop, record it
            if (ROOT_CAUSE_TYPES.contains(sourceNode.getNodeType()) && path.size() > 1) {
                foundPaths.add(new ArrayList<>(path));
                foundWeights.add(new ArrayList<>(weights));
            }

            // Continue DFS
            dfsBackward(sourceId, path, weights, visited, incomingEdges, nodeMap, foundPaths, foundWeights);

            path.removeLast();
            weights.removeLast();
        }

        visited.remove(currentId);
    }

    @Transactional(readOnly = true)
    public BlastRadiusResult computeBlastRadius(UUID incidentId, UUID rootNodeId) {
        incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentService.IncidentNotFoundException(incidentId));

        var rootNode = nodeRepository.findById(rootNodeId)
            .filter(n -> n.getIncidentId().equals(incidentId))
            .orElseThrow(() -> new NodeNotFoundException(rootNodeId));

        var edges = edgeRepository.findByIncidentId(incidentId);

        // Build outgoing-edge adjacency map
        Map<UUID, List<CorrelationEdge>> outgoingEdges = new HashMap<>();
        for (var e : edges) {
            if (TRAVERSABLE_EDGE_TYPES.contains(e.getEdgeType())) {
                outgoingEdges.computeIfAbsent(e.getSourceNodeId(), k -> new ArrayList<>()).add(e);
            }
        }

        // BFS forward
        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new LinkedList<>();
        queue.add(rootNodeId);
        visited.add(rootNodeId);

        List<GraphNodeResult> affected = new ArrayList<>();
        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            var outgoing = outgoingEdges.getOrDefault(current, List.of());
            for (var edge : outgoing) {
                UUID targetId = edge.getTargetNodeId();
                if (!visited.contains(targetId)) {
                    visited.add(targetId);
                    queue.add(targetId);
                    nodeRepository.findById(targetId)
                        .map(GraphNodeResult::from)
                        .ifPresent(affected::add);
                }
            }
        }

        return new BlastRadiusResult(GraphNodeResult.from(rootNode), affected, affected.size());
    }

    private int seedAlertNodes(UUID incidentId, String tenantId) {
        var alerts = alertRepository.findByIncidentId(incidentId);
        int seeded = 0;

        for (Alert alert : alerts) {
            // Check if already seeded (idempotent)
            var existing = nodeRepository.findByIncidentIdAndReferenceId(incidentId, alert.getId());
            if (!existing.isEmpty()) continue;

            var node = new CorrelationNode(
                incidentId, CorrelationNodeType.ALERT, alert.getTitle(),
                alert.getDescription(), alert.getId(), alert.getSource(), "{}"
            );
            node.setTenantId(tenantId);
            nodeRepository.save(node);
            seeded++;
        }

        if (seeded > 0) {
            var timelineEvent = IncidentEvent.graphSeeded(incidentId, seeded);
            timelineEvent.setTenantId(tenantId);
            incidentEventRepository.save(timelineEvent);
        }

        return seeded;
    }

    private String serializeMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public static class NodeNotFoundException extends RuntimeException {
        public NodeNotFoundException(UUID nodeId) {
            super("Graph node not found: " + nodeId);
        }
    }
}
