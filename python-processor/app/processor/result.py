from dataclasses import dataclass
from typing import Optional

@dataclass
class ProcessingResult:
    incident_id: int
    incident_key: str
    risk_level: str
    remediation_eligible: bool
    remediation_attempted: bool
    remediation_successful: bool
    reason: str
