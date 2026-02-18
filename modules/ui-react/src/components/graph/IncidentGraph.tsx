import { useEffect, useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  type Node,
  type Edge,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

import { fetchGraph, fetchRootCausePaths, type CorrelationGraph, type RootCausePathDto } from '../../api/graphApi';
import { EDGE_COLORS, EDGE_ANIMATED, NODE_COLORS } from './nodeStyles';
import CorrelationNodeComponent from './CorrelationNodeComponent';
import { useGraphLayout } from './useGraphLayout';

const nodeTypes = { correlation: CorrelationNodeComponent };

export default function IncidentGraph() {
  const { incidentId } = useParams<{ incidentId: string }>();
  const [graph, setGraph] = useState<CorrelationGraph | null>(null);
  const [paths, setPaths] = useState<RootCausePathDto[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!incidentId) return;
    Promise.all([
      fetchGraph(incidentId),
      fetchRootCausePaths(incidentId),
    ])
      .then(([g, p]) => { setGraph(g); setPaths(p); })
      .catch((e) => setError(e.message));
  }, [incidentId]);

  const rawNodes: Node[] = useMemo(() => {
    if (!graph) return [];
    return graph.nodes.map((n) => ({
      id: n.id,
      type: 'correlation',
      position: { x: 0, y: 0 },
      data: { label: n.label, nodeType: n.nodeType, description: n.description },
    }));
  }, [graph]);

  const rawEdges: Edge[] = useMemo(() => {
    if (!graph) return [];
    return graph.edges.map((e) => ({
      id: e.id,
      source: e.sourceNodeId,
      target: e.targetNodeId,
      label: `${e.edgeType} (${e.weight.toFixed(2)})`,
      style: { stroke: EDGE_COLORS[e.edgeType] || '#9ca3af' },
      animated: EDGE_ANIMATED[e.edgeType] || false,
    }));
  }, [graph]);

  const { nodes, edges } = useGraphLayout(rawNodes, rawEdges);

  if (error) return <div style={{ padding: 24, color: '#ef4444' }}>Error: {error}</div>;
  if (!graph) return <div style={{ padding: 24 }}>Loading graph...</div>;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', fontFamily: 'system-ui, sans-serif' }}>
      <div style={{ padding: '12px 24px', borderBottom: '1px solid #e5e7eb' }}>
        <h2 style={{ margin: 0, fontSize: 18 }}>Correlation Graph — {incidentId}</h2>
        <span style={{ color: '#6b7280', fontSize: 13 }}>
          {graph.nodes.length} nodes, {graph.edges.length} edges
        </span>
      </div>

      <div style={{ flex: 1 }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          nodeTypes={nodeTypes}
          fitView
        >
          <Background />
          <Controls />
          <MiniMap
            nodeColor={(n) => {
              const data = n.data as { nodeType?: string };
              return NODE_COLORS[data.nodeType || ''] || '#6b7280';
            }}
          />
        </ReactFlow>
      </div>

      {paths.length > 0 && (
        <div style={{ padding: '12px 24px', borderTop: '1px solid #e5e7eb', maxHeight: 200, overflowY: 'auto' }}>
          <h3 style={{ margin: '0 0 8px', fontSize: 14 }}>Root Cause Paths</h3>
          {paths.map((p, i) => (
            <div key={i} style={{ marginBottom: 8, fontSize: 13 }}>
              <strong>#{i + 1}</strong> (score: {p.score.toFixed(3)}):{' '}
              {p.nodeLabels.join(' → ')}
              <div style={{ color: '#6b7280', fontSize: 12 }}>{p.explanation}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
