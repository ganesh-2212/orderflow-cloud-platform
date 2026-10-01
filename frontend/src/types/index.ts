export interface HealthStatus {
  status: 'UP' | 'DOWN' | 'UNKNOWN';
}

export interface IncidentStats {
  total: number;
  open: number;
  investigating: number;
  mitigating: number;
  resolved: number;
  closed: number;
  critical: number;
  high: number;
  medium: number;
  low: number;
}

export interface Incident {
  incidentKey: string;
  service: string;
  category: string;
  severity: string;
  status: string;
  retryCount: number;
  createdAt: string;
}

export interface MetricData {
  name: string;
  measurements: {
    statistic: string;
    value: number;
  }[];
}
