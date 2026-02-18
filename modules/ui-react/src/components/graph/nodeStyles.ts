export const NODE_COLORS: Record<string, string> = {
  ALERT: '#ef4444',
  METRIC_ANOMALY: '#f97316',
  LOG_CLUSTER: '#eab308',
  DEPLOY: '#3b82f6',
  SERVICE: '#8b5cf6',
  DEPENDENCY: '#6b7280',
};

export const NODE_BG_COLORS: Record<string, string> = {
  ALERT: '#fef2f2',
  METRIC_ANOMALY: '#fff7ed',
  LOG_CLUSTER: '#fefce8',
  DEPLOY: '#eff6ff',
  SERVICE: '#f5f3ff',
  DEPENDENCY: '#f9fafb',
};

export const EDGE_COLORS: Record<string, string> = {
  TIME_CORRELATION: '#9ca3af',
  DEPENDS_ON: '#3b82f6',
  CAUSAL_HINT: '#ef4444',
  SAME_RELEASE: '#8b5cf6',
};

export const EDGE_ANIMATED: Record<string, boolean> = {
  TIME_CORRELATION: false,
  DEPENDS_ON: false,
  CAUSAL_HINT: true,
  SAME_RELEASE: false,
};
