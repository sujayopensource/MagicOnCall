import { Handle, Position, type NodeProps } from '@xyflow/react';
import { NODE_COLORS, NODE_BG_COLORS } from './nodeStyles';

interface CorrelationNodeData {
  label: string;
  nodeType: string;
  description?: string | null;
  [key: string]: unknown;
}

export default function CorrelationNodeComponent({ data }: NodeProps) {
  const nodeData = data as unknown as CorrelationNodeData;
  const borderColor = NODE_COLORS[nodeData.nodeType] || '#6b7280';
  const bgColor = NODE_BG_COLORS[nodeData.nodeType] || '#f9fafb';

  return (
    <div
      style={{
        padding: '10px 14px',
        borderRadius: 8,
        border: `2px solid ${borderColor}`,
        backgroundColor: bgColor,
        minWidth: 140,
        fontSize: 12,
        fontFamily: 'system-ui, sans-serif',
      }}
    >
      <Handle type="target" position={Position.Top} />
      <div
        style={{
          fontSize: 9,
          fontWeight: 700,
          textTransform: 'uppercase',
          letterSpacing: '0.05em',
          color: borderColor,
          marginBottom: 4,
        }}
      >
        {nodeData.nodeType.replace('_', ' ')}
      </div>
      <div style={{ fontWeight: 600, color: '#1f2937' }}>{nodeData.label}</div>
      {nodeData.description && (
        <div style={{ color: '#6b7280', marginTop: 4, fontSize: 11 }}>
          {nodeData.description}
        </div>
      )}
      <Handle type="source" position={Position.Bottom} />
    </div>
  );
}
