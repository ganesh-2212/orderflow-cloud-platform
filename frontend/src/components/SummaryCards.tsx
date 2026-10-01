import React from 'react';
import type { IncidentStats } from '../types';

interface SummaryCardsProps {
  stats: IncidentStats | null;
}

export const SummaryCards: React.FC<SummaryCardsProps> = ({ stats }) => {
  return (
    <div className="summary-cards">
      <div className="card">
        <h3>Total Incidents</h3>
        <div className="value">{stats?.total ?? '-'}</div>
      </div>
      <div className="card open-incidents">
        <h3>Open Incidents</h3>
        <div className="value">{stats?.open ?? '-'}</div>
      </div>
      <div className="card high-critical">
        <h3>High/Critical</h3>
        <div className="value">{(stats?.high ?? 0) + (stats?.critical ?? 0) || '-'}</div>
      </div>
      <div className="card active-remediation">
        <h3>Active Remediation</h3>
        <div className="value">{stats?.mitigating ?? '-'}</div>
      </div>
    </div>
  );
};
