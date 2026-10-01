import React from 'react';

interface CustomMetricsProps {
  metrics: Record<string, number | null>;
}

export const CustomMetrics: React.FC<CustomMetricsProps> = ({ metrics }) => {
  return (
    <div className="custom-metrics-section dashboard-panel">
      <h2>Service Metrics</h2>
      
      <div className="metrics-grid">
        <div className="metric-group">
          <h3>Order Service</h3>
          <div className="metric-item">
            <span>Orders Created:</span> <span>{metrics['orderflow.orders.created'] ?? '-'}</span>
          </div>
          <div className="metric-item">
            <span>Orders Failed:</span> <span>{metrics['orderflow.orders.failed'] ?? '-'}</span>
          </div>
        </div>

        <div className="metric-group">
          <h3>Inventory Service</h3>
          <div className="metric-item">
            <span>Reservations:</span> <span>{metrics['orderflow.inventory.reservations'] ?? '-'}</span>
          </div>
          <div className="metric-item">
            <span>Failures:</span> <span>{metrics['orderflow.inventory.failures'] ?? '-'}</span>
          </div>
          <div className="metric-item">
            <span>Releases:</span> <span>{metrics['orderflow.inventory.releases'] ?? '-'}</span>
          </div>
        </div>

        <div className="metric-group">
          <h3>Fulfillment Service</h3>
          <div className="metric-item">
            <span>Created:</span> <span>{metrics['orderflow.fulfillments.created'] ?? '-'}</span>
          </div>
          <div className="metric-item">
            <span>Shipped:</span> <span>{metrics['orderflow.fulfillments.shipped'] ?? '-'}</span>
          </div>
          <div className="metric-item">
            <span>Delivered:</span> <span>{metrics['orderflow.fulfillments.delivered'] ?? '-'}</span>
          </div>
        </div>

        <div className="metric-group">
          <h3>Incident Service</h3>
          <div className="metric-item">
            <span>Incidents Created:</span> <span>{metrics['orderflow.incidents.created'] ?? '-'}</span>
          </div>
          <div className="metric-item">
            <span>Resolved:</span> <span>{metrics['orderflow.incidents.resolved'] ?? '-'}</span>
          </div>
          <div className="metric-item">
            <span>Remediation Attempts:</span> <span>{metrics['orderflow.incidents.remediation.attempts'] ?? '-'}</span>
          </div>
        </div>
      </div>
    </div>
  );
};
