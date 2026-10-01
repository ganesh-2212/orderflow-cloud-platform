from dataclasses import dataclass
from typing import Optional

@dataclass
class Incident:
    id: int
    incidentKey: str
    serviceName: str
    resourceId: str
    category: str
    severity: str
    status: str
    title: str
    description: str
    errorCode: Optional[str]
    errorMessage: Optional[str]
    retryCount: int
    resolutionNotes: Optional[str]
    createdAt: str
    updatedAt: str
    resolvedAt: Optional[str]

    @classmethod
    def from_dict(cls, data: dict) -> 'Incident':
        return cls(
            id=data.get('id', 0),
            incidentKey=data.get('incidentKey', ''),
            serviceName=data.get('serviceName', ''),
            resourceId=data.get('resourceId', ''),
            category=data.get('category', ''),
            severity=data.get('severity', ''),
            status=data.get('status', ''),
            title=data.get('title', ''),
            description=data.get('description', ''),
            errorCode=data.get('errorCode'),
            errorMessage=data.get('errorMessage'),
            retryCount=data.get('retryCount', 0),
            resolutionNotes=data.get('resolutionNotes'),
            createdAt=data.get('createdAt', ''),
            updatedAt=data.get('updatedAt', ''),
            resolvedAt=data.get('resolvedAt')
        )
