export const API_CONFIG = {
  orderService: '/order',
  inventoryService: '/inventory',
  fulfillmentService: '/fulfillment',
  incidentService: '/incident',
};

export async function fetchWithErrorHandling<T>(url: string, options?: RequestInit): Promise<T> {
  try {
    const response = await fetch(url, options);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error(`Error fetching ${url}:`, error);
    throw error;
  }
}
