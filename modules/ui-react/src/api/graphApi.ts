const API_BASE = '/api/v1/incidents';
const API_KEY = 'moc-dev-key-001';
const TENANT = 'tenant-default';

interface GraphNode {
  id: string;
  incidentId: string;
  nodeType: string;
  label: string;
  description: string | null;
  referenceId: string | null;
  source: string | null;
  metadata: string | null;
  createdAt: string;
}

interface GraphEdge {
  id: string;
  incidentId: string;
  sourceNodeId: string;
  targetNodeId: string;
  edgeType: string;
  weight: number;
  reason: string | null;
  metadata: string | null;
  createdAt: string;
}

export interface CorrelationGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

export interface RootCausePathDto {
  nodeIds: string[];
  nodeLabels: string[];
  score: number;
  explanation: string;
}

export interface BlastRadiusDto {
  rootCauseNode: GraphNode;
  affectedNodes: GraphNode[];
  totalAffected: number;
}

export type { GraphNode, GraphEdge };

const headers = {
  'Content-Type': 'application/json',
  'X-Api-Key': API_KEY,
  'X-Customer-Id': TENANT,
};

export async function fetchGraph(incidentId: string): Promise<CorrelationGraph> {
  const res = await fetch(`${API_BASE}/${incidentId}/graph`, { headers });
  if (!res.ok) throw new Error(`Failed to fetch graph: ${res.status}`);
  return res.json();
}

export async function fetchRootCausePaths(incidentId: string, maxPaths = 3): Promise<RootCausePathDto[]> {
  const res = await fetch(`${API_BASE}/${incidentId}/root-cause-paths?maxPaths=${maxPaths}`, { headers });
  if (!res.ok) throw new Error(`Failed to fetch root cause paths: ${res.status}`);
  return res.json();
}

export async function fetchBlastRadius(incidentId: string, nodeId: string): Promise<BlastRadiusDto> {
  const res = await fetch(`${API_BASE}/${incidentId}/blast-radius/${nodeId}`, { headers });
  if (!res.ok) throw new Error(`Failed to fetch blast radius: ${res.status}`);
  return res.json();
}
