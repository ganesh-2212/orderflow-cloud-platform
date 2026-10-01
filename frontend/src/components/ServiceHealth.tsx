import React from 'react';
import type { HealthStatus } from '../types';

interface ServiceHealthProps {
  health: Record<string, HealthStatus>;
}

export const ServiceHealth: React.FC<ServiceHealthProps> = ({ health }) => {
  const services = [
    { key: 'orderService', name: 'Order Service' },
    { key: 'inventoryService', name: 'Inventory Service' },
    { key: 'fulfillmentService', name: 'Fulfillment Service' },
    { key: 'incidentService', name: 'Incident Service' },
  ];

  return (
    <div className="service-health-section dashboard-panel">
      <h2>Service Health</h2>
      <div className="health-grid">
        {services.map(svc => {
          const status = health[svc.key]?.status || 'UNKNOWN';
          return (
            <div key={svc.key} className={`health-card status-${status.toLowerCase()}`}>
              <div className="service-name">{svc.name}</div>
              <div className="service-status">
                <span className="status-dot"></span>
                {status}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
