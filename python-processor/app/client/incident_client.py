import requests
import logging
from typing import List
from app.models.incident import Incident

logger = logging.getLogger(__name__)

class IncidentClientError(Exception):
    pass

class IncidentClient:
    def __init__(self, base_url: str, timeout: int = 5):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout

    def get_open_incidents(self) -> List[Incident]:
        url = f"{self.base_url}/api/v1/incidents/status/OPEN"
        try:
            response = requests.get(url, timeout=self.timeout)
            response.raise_for_status()
            data = response.json()
            return [Incident.from_dict(item) for item in data]
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to fetch open incidents: {str(e)}")
            raise IncidentClientError(f"HTTP request failed: {str(e)}")

    def trigger_remediation(self, incident_id: int) -> bool:
        url = f"{self.base_url}/api/v1/incidents/{incident_id}/remediation"
        payload = {"action": "RETRY_OPERATION"}
        try:
            response = requests.post(url, json=payload, timeout=self.timeout)
            
            if response.status_code in (200, 201):
                return True
            elif response.status_code == 409:
                logger.warning(f"Remediation conflict (409) for incident {incident_id}")
                return False
            
            response.raise_for_status()
            return False
            
        except requests.exceptions.RequestException as e:
            logger.error(f"Failed to trigger remediation for incident {incident_id}: {str(e)}")
            raise IncidentClientError(f"Remediation request failed: {str(e)}")
