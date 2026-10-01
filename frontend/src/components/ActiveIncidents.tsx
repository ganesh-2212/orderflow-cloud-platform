import React from 'react';
import type { Incident } from '../types';

interface ActiveIncidentsProps {
  incidents: Incident[];
}

export const ActiveIncidents: React.FC<ActiveIncidentsProps> = ({ incidents }) => {
  return (
    <div className="active-incidents-section dashboard-panel">
      <h2>Active Incidents</h2>
      {incidents.length === 0 ? (
        <p>No active incidents.</p>
      ) : (
        <table className="incidents-table">
          <thead>
            <tr>
              <th>Incident Key</th>
              <th>Service</th>
              <th>Category</th>
              <th>Severity</th>
              <th>Status</th>
              <th>Retry Count</th>
              <th>Created At</th>
            </tr>
          </thead>
          <tbody>
            {incidents.map((inc, i) => (
              <tr key={i}>
                <td>{inc.incidentKey}</td>
                <td>{inc.service}</td>
                <td>{inc.category}</td>
                <td><span className={`severity-badge ${inc.severity.toLowerCase()}`}>{inc.severity}</span></td>
                <td><span className={`status-badge ${inc.status.toLowerCase()}`}>{inc.status}</span></td>
                <td>{inc.retryCount}</td>
                <td>{new Date(inc.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};
