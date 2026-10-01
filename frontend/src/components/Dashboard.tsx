import React, { useState, useEffect } from 'react';
import { Header } from './Header';
import { SummaryCards } from './SummaryCards';
import { ServiceHealth } from './ServiceHealth';
import { ActiveIncidents } from './ActiveIncidents';
import { CustomMetrics } from './CustomMetrics';
import type { HealthStatus, IncidentStats, Incident } from '../types';
import { checkServiceHealth } from '../api/healthApi';
import { getIncidentStats, getActiveIncidents } from '../api/incidentApi';
import { getMetric } from '../api/metricsApi';
import { API_CONFIG } from '../api/config';

export const Dashboard: React.FC = () => {
  const [isUpdating, setIsUpdating] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<string>('--:--:--');
  const [health, setHealth] = useState<Record<string, HealthStatus>>({});
  const [incidentStats, setIncidentStats] = useState<IncidentStats | null>(null);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [metrics, setMetrics] = useState<Record<string, number | null>>({});

  const fetchData = async () => {
    setIsUpdating(true);
    try {
      // 1. Health Checks
      const [orderHealth, inventoryHealth, fulfillmentHealth, incidentHealth] = await Promise.all([
        checkServiceHealth('orderService'),
        checkServiceHealth('inventoryService'),
        checkServiceHealth('fulfillmentService'),
        checkServiceHealth('incidentService'),
      ]);
      setHealth({
        orderService: orderHealth,
        inventoryService: inventoryHealth,
        fulfillmentService: fulfillmentHealth,
        incidentService: incidentHealth,
      });

      // 2. Incident Statistics
      try {
        const stats = await getIncidentStats();
        setIncidentStats(stats);
      } catch (e) {
        setIncidentStats(null);
      }

      // 3. Active Incidents
      try {
        const activeIncs = await getActiveIncidents();
        const sorted = (Array.isArray(activeIncs) ? activeIncs : []).sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        setIncidents(sorted);
      } catch (e) {
        setIncidents([]);
      }

      // 4. Custom Metrics
      const mKeys = [
        { s: 'orderService', m: 'orderflow.orders.created' },
        { s: 'orderService', m: 'orderflow.orders.failed' },
        { s: 'inventoryService', m: 'orderflow.inventory.reservations' },
        { s: 'inventoryService', m: 'orderflow.inventory.failures' },
        { s: 'inventoryService', m: 'orderflow.inventory.releases' },
        { s: 'fulfillmentService', m: 'orderflow.fulfillments.created' },
        { s: 'fulfillmentService', m: 'orderflow.fulfillments.shipped' },
        { s: 'fulfillmentService', m: 'orderflow.fulfillments.delivered' },
        { s: 'incidentService', m: 'orderflow.incidents.created' },
        { s: 'incidentService', m: 'orderflow.incidents.resolved' },
        { s: 'incidentService', m: 'orderflow.incidents.remediation.attempts' }
      ];

      const metricsResult: Record<string, number | null> = {};
      await Promise.all(
        mKeys.map(async (k) => {
          metricsResult[k.m] = await getMetric(k.s as keyof typeof API_CONFIG, k.m);
        })
      );
      setMetrics(metricsResult);
      
    } catch (error) {
      console.error('Dashboard fetch error', error);
    } finally {
      setIsUpdating(false);
      const now = new Date();
      setLastUpdated(now.toLocaleTimeString());
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 15000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="dashboard-container">
      <Header lastUpdated={lastUpdated} isUpdating={isUpdating} />
      <div className="dashboard-content">
        <SummaryCards stats={incidentStats} />
        <div className="dashboard-grid">
          <div className="left-column">
            <ServiceHealth health={health} />
            <CustomMetrics metrics={metrics} />
          </div>
          <div className="right-column">
            <ActiveIncidents incidents={incidents} />
          </div>
        </div>
      </div>
    </div>
  );
};
