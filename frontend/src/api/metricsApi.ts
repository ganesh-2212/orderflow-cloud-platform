import { API_CONFIG, fetchWithErrorHandling } from './config';
import type { MetricData } from '../types';

export const getMetric = async (serviceName: keyof typeof API_CONFIG, metricName: string): Promise<number | null> => {
  try {
    const data = await fetchWithErrorHandling<MetricData>(`${API_CONFIG[serviceName]}/actuator/metrics/${metricName}`);
    if (data && data.measurements && data.measurements.length > 0) {
      return data.measurements[0].value;
    }
    return null;
  } catch (error) {
    return null;
  }
};
