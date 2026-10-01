import { API_CONFIG, fetchWithErrorHandling } from './config';
import type { Incident, IncidentStats } from '../types';

export const getIncidentStats = async (): Promise<IncidentStats> => {
  return await fetchWithErrorHandling<IncidentStats>(`${API_CONFIG.incidentService}/api/v1/incidents/statistics`);
};

export const getActiveIncidents = async (): Promise<Incident[]> => {
  // Assuming the endpoint returns a list or a paginated response. If paginated, map it correctly in the future.
  return await fetchWithErrorHandling<Incident[]>(`${API_CONFIG.incidentService}/api/v1/incidents`);
};
