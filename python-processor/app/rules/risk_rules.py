from typing import Tuple
from app.models.incident import Incident

def evaluate_risk(incident: Incident, max_attempts: int = 3) -> Tuple[str, bool, str]:
    if incident.status != "OPEN":
        return "UNKNOWN", False, "Incident is not in OPEN status"

    if incident.retryCount >= max_attempts:
        return incident.severity, False, "Maximum remediation attempts reached"

    if incident.severity == "CRITICAL":
        return "CRITICAL", False, "CRITICAL incidents are not eligible for auto-remediation"

    if incident.severity == "HIGH":
        eligible_categories = ["SERVICE_UNAVAILABLE", "TIMEOUT", "INVENTORY_FAILURE", "FULFILLMENT_FAILURE"]
        if incident.category in eligible_categories:
            return "HIGH", True, f"High-severity {incident.category} eligible for bounded retry"
        return "HIGH", False, f"High-severity {incident.category} not eligible for auto-remediation"

    if incident.severity == "MEDIUM":
        return "MEDIUM", True, "Medium-severity incident eligible for bounded retry"

    if incident.severity == "LOW":
        return "LOW", False, "Low-severity incidents do not require auto-remediation"

    return "UNKNOWN", False, "Unable to classify incident risk"
