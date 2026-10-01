import { API_CONFIG, fetchWithErrorHandling } from './config';
import type { HealthStatus } from '../types';

export const checkServiceHealth = async (serviceName: keyof typeof API_CONFIG): Promise<HealthStatus> => {
  try {
    return await fetchWithErrorHandling<HealthStatus>(`${API_CONFIG[serviceName]}/actuator/health`);
  } catch (error) {
    return { status: 'DOWN' };
  }
};
